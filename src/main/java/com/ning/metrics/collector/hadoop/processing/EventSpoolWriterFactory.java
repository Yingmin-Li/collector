/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.metrics.collector.hadoop.processing;

import com.ning.arecibo.jmx.Monitored;
import com.ning.metrics.collector.binder.config.CollectorConfig;
import com.ning.metrics.serialization.writer.CallbackHandler;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventHandler;
import com.ning.metrics.serialization.writer.EventWriter;
import com.ning.metrics.serialization.writer.SyncType;
import com.ning.metrics.serialization.writer.ThresholdEventWriter;

import com.google.inject.Inject;
import com.mogwee.executors.FailsafeScheduledExecutor;
import com.mogwee.executors.LoggingExecutor;
import com.mogwee.executors.NamedThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.Managed;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventSpoolWriterFactory implements PersistentWriterFactory
{
    private static final Logger log = LoggerFactory.getLogger(EventSpoolWriterFactory.class);
    private final CollectorConfig config;
    private final AtomicBoolean flushEnabled;
    private final Set<EventSpoolProcessor> eventSpoolProcessorSet;
    private long cutoffTime = 7200000L;
    private final int NTHREDS = 10;
    private final int executorShutdownTimeOut = 5;
    private final ExecutorService executorService;
    
    @Inject
    public EventSpoolWriterFactory(final Set<EventSpoolProcessor> eventSpoolProcessorSet, final CollectorConfig config)
    {
        this.eventSpoolProcessorSet = eventSpoolProcessorSet;
        this.config = config;
        this.flushEnabled = new AtomicBoolean(config.isFlushEnabled());
        executorService = new LoggingExecutor(0, NTHREDS , 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new NamedThreadFactory("EventSpool-Processor-Threads"));
    }

    @Override
    public EventWriter createPersistentWriter(final WriterStats stats, final SerializationType serializationType, final String eventName, final String eventOutputDirectory)
    {
        final LocalSpoolManager spoolManager = new LocalSpoolManager(config, eventName, serializationType, eventOutputDirectory);

        final EventWriter eventWriter = new DiskSpoolEventWriter(new EventHandler()
        {
            private int flushCount = 0;

            @Override
            public void handle(final File file, final CallbackHandler handler)
            {
                if (!flushEnabled.get()) {
                    return; // Flush Disabled?
                }

                try {
                    final String outputPath = spoolManager.toHadoopPath(flushCount);
                    for(final EventSpoolProcessor eventSpoolProcessor : eventSpoolProcessorSet)
                    {
                        executorService.submit(new Callable<Boolean>() {

                            @Override
                            public Boolean call() throws Exception
                            {
                                eventSpoolProcessor.processEventFile(file, outputPath);
                                return true;
                            }
                            
                        });
                        
                    }
                }
                catch (Exception e) {
                    handler.onError(e, file);
                    // Increment flush count in case the file was created on HDFS
                    flushCount++;
                    return;
                }

                handler.onSuccess(file);
                stats.registerHdfsFlush();
                flushCount++;
            }
        }, spoolManager.getSpoolDirectoryPath(), config.isFlushEnabled(), config.getFlushIntervalInSeconds(), new FailsafeScheduledExecutor(1, eventOutputDirectory + "-EventSpool-writer"),
            SyncType.valueOf(config.getSyncType()), config.getSyncBatchSize(), config.getCompressionCodec(), serializationType.getSerializer());
        return new ThresholdEventWriter(eventWriter, config.getMaxUncommittedWriteCount(), config.getMaxUncommittedPeriodInSeconds());
    }

    /**
     * In case the EventWriter responsible for a certain queue goes away (e.g. collector restarted),
     * we need to process manually all files left below.
     * This includes all files in all directories under the spool directory, but the ones in _tmp. _tmp are files being written,
     * since they may not have been be closed, we don't want to upload garbage.
     *
     * @throws java.io.IOException Exception when writing to HDFS
     * @see <a href="http://en.wikipedia.org/wiki/Thank_God,_It's_Doomsday">Left Below</a>
     */
    @Override
    @Managed(description = "Process all local files files")
    public void processLeftBelowFiles() throws IOException
    {
        log.info("Processing files left below");
        // We are going to flush all files that are not being written (not in the _tmp directory) and then delete
        // empty directories. We can't distinguish older directories vs ones currently in use except by timestamp.
        // We record candidates first, delete the files, and then delete the empty directories among the candidates.
        final Collection<File> potentialOldDirectories = LocalSpoolManager.findOldSpoolDirectories(config.getSpoolDirectoryName(), getCutoffTime());

        final HashMap<String, Integer> flushesPerEvent = new HashMap<String, Integer>();
        for (final File oldDirectory : potentialOldDirectories) {
            // Ignore _tmp, files may be corrupted (not closed properly)
            for (final File file : LocalSpoolManager.findFilesInSpoolDirectory(oldDirectory)) {
                final LocalSpoolManager spoolManager;
                try {
                    spoolManager = new LocalSpoolManager(config, oldDirectory);
                }
                catch (IllegalArgumentException e) {
                    log.warn(String.format("Skipping invalid local directory: %s", file.getAbsolutePath()));
                    continue;
                }

                incrementFlushCount(flushesPerEvent, spoolManager.getEventName());
                final String outputPath = spoolManager.toHadoopPath(flushesPerEvent.get(spoolManager.getEventName()));
                for(EventSpoolProcessor eventSpoolProcessor : eventSpoolProcessorSet)
                {
                    eventSpoolProcessor.processEventFile(file, outputPath);
                }

                if (!file.delete()) {
                    log.warn(String.format("Exception cleaning up left below file: %s. We might have DUPS!", file.toString()));
                }
            }
        }

        LocalSpoolManager.cleanupOldSpoolDirectories(potentialOldDirectories);
    }

    @Override
    public void close()
    {
        executorService.shutdown();
        
        try {
            executorService.awaitTermination(executorShutdownTimeOut, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        executorService.shutdownNow();
        
        log.info("Processing old files and quarantine directories");
        try {
            processLeftBelowFiles();
        }
        catch (IOException e) {
            log.warn("Got IOException trying to process left below files: " + e.getLocalizedMessage());
        }

        // Give some time for the flush to happen
        final File spoolDirectory = new File(config.getSpoolDirectoryName());
        int nbOfSleeps = 0;
        int numberOfLocalFiles = LocalSpoolManager.findFilesInSpoolDirectory(spoolDirectory).size();
        while (numberOfLocalFiles > 0 && nbOfSleeps < 10) {
            log.info(String.format("%d more files are left to be flushed, sleeping to give them a chance", numberOfLocalFiles));
            try {
                Thread.sleep(5000L);
                numberOfLocalFiles = LocalSpoolManager.findFilesInSpoolDirectory(spoolDirectory).size();
                nbOfSleeps++;
            }
            catch (InterruptedException e) {
                log.warn(String.format("Interrupted while waiting for files to be flushed to HDFS. This means that [%s] still contains data!", config.getSpoolDirectoryName()));
                break;
            }
        }

        if (numberOfLocalFiles > 0) {
            log.warn(String.format("Giving up while waiting for files to be flushed to HDFS. Files not flushed: %s", LocalSpoolManager.findFilesInSpoolDirectory(spoolDirectory)));
        }
        else {
            log.info("All local files have been flushed");
        }
    }
    
    /**
     * Increment flushes count for this event
     *
     * @param flushesPerEvent global HashMap keeping state
     * @param eventName       name of the event
     */
    private void incrementFlushCount(final HashMap<String, Integer> flushesPerEvent, final String eventName)
    {
        final Integer flushes = flushesPerEvent.get(eventName);
        if (flushes == null) {
            flushesPerEvent.put(eventName, 0);
        }
        flushesPerEvent.put(eventName, flushesPerEvent.get(eventName) + 1);
    }

    /**
     * When processing asynchronously files in the diskspool, how old the files should be?
     * Candidates are directories last modified more than 2 hours ago
     *
     * @return cutoff time in milliseconds
     */
    @Monitored(description = "Cutoff time for files to be sent to Spool Processors")
    public long getCutoffTime()
    {
        return cutoffTime;
    }

    @Managed(description = "Set the cutoff time")
    public void setCutoffTime(final long cutoffTime)
    {
        this.cutoffTime = cutoffTime;
    }

    @Managed(description = "Whether files should be flushed")
    public AtomicBoolean getFlushEnabled()
    {
        return flushEnabled;
    }

    @Managed(description = "Enable flush")
    public void enableFlush()
    {
        flushEnabled.set(true);
    }

    @Managed(description = "Disable Flush")
    public void disableFlush()
    {
        flushEnabled.set(false);
    }

    @Monitored(description = "Number of local files not yet pushed to Spool Processors")
    public int nbLocalFiles()
    {
        return LocalSpoolManager.findFilesInSpoolDirectory(new File(config.getSpoolDirectoryName())).size();
    }

}
