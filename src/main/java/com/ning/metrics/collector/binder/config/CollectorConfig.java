/*
 * Copyright 2010-2011 Ning, Inc.
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

package com.ning.metrics.collector.binder.config;

import com.ning.metrics.serialization.writer.CompressionCodec;

import com.googlecode.jsendnsca.encryption.Encryption;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.DefaultNull;
import org.skife.config.Description;
import org.skife.config.TimeSpan;

public interface CollectorConfig
{
    @Config("collector.dfs.block.size")
    @Default("134217728")
    long getHadoopBlockSize();

    @Config("collector.hadoop.ugi")
    @Default("nobody,nobody")
    String getHadoopUgi();

    // Whether to forward events to ActiveMQ

    @Config("collector.activemq.enabled")
    @Default("false")
    boolean isActiveMQEnabled();

    // ActiveMQ uri

    @Config("collector.activemq.uri")
    @DefaultNull
    String getActiveMQUri();

    // Events types to forward to ActiveMQ (comma delimited)

    @Config("collector.activemq.events")
    @DefaultNull
    String getActiveMQEventsToCollect();

    // ActiveMQ messages TTL in milliseconds, default 5 minutes

    @Config({"collector.activemq.${category}.messagesTTLmilliseconds",
             "collector.activemq.messagesTTLmilliseconds" })
    @Default("300000")
    int getMessagesTTLMilliseconds();
    
    // Length of the internal buffer for passing events of a specific type to activemq
    @Config({"collector.activemq.${category}.bufferLength",
             "collector.activemq.bufferLength"})
    @Default("10000")
    int getActiveMQBufferLength();

    // Number of senders constructed for specific category; usually one
    // is fine for low/medium volume; higher for high-volume ones (2 - 4)
    @Config({"collector.activemq.${category}.numSendersPerCategory",
             "collector.activemq.numSendersPerCategory"})
    @Default("1")
    int getActiveMQNumSendersPerCategory();

    // Whether we use BytesMessage (true), or TextMessage (false)
    // when sending ActiveMQ notifications
    // NOTE: global setting, no per-topic overrides
    @Config("collector.activemq.useBytesMessage")
    @Default("false")
    boolean getActiveMQUseBytesMessage();    

    // Whether we are to use async send or not
    // note: currently used in global way, thus no per-category override
    @Config("collector.activemq.asyncSend")
    @Default("false")
    boolean getActiveMQUseAsyncSend();    
    
    @Config("collector.scribe.enabled")
    @Default("true")
    boolean isScribeCollectionEnabled();

    // Scribe port

    @Config("collector.scribe.port")
    @Default("7911")
    int getScribePort();

    @Config("collector.max-event-queue-size")
    @Default("200000")
    int getMaxQueueSize();

    @Config("collector.event-routes.persistent")
    @Default("true")
    boolean isHdfsWriterEnabled();

    @Config("collector.event-end-point.rate-window-size-minutes")
    @Default("5")
    int getRateWindowSizeMinutes();

    //------------------- Spooling -------------------//

    /**
     * Directory for the collector to buffer events before writing them to HDFS
     *
     * @return the directory path
     */
    @Config("collector.diskspool.path")
    @Default(".diskspool")
    String getSpoolDirectoryName();

    /**
     * If false, events will not be periodically written to HDFS
     *
     * @return whether to send events buffered locally
     */
    @Config("collector.diskspool.enabled")
    @Default("true")
    boolean isFlushEnabled();

    /**
     * Delay between flushes (in seconds).
     * This is used in the DiskSpoolEventWriter (delay between flushes).
     *
     * @return delay between flushes to HDFS
     * @see com.ning.metrics.serialization.writer.DiskSpoolEventWriter
     */
    @Config("collector.diskspool.flush-interval-seconds")
    @Default("30")
    int getFlushIntervalInSeconds();

    /**
     * Type of outputter to use when spooling: NONE, FLUSH, or SYNC
     *
     * @return the String representation of the SyncType
     */
    @Config("collector.diskspool.synctype")
    @Default("NONE")
    String getSyncType();

    /**
     * Size of the batch for the sync type parameter
     *
     * @return the number of events to buffer before calling flush or sync
     */
    @Config("collector.diskspool.batch-size")
    @Default("50")
    int getSyncBatchSize();

    /**
     * Maximum number of events in the file being written (_tmp directory).
     * <p/>
     * Maximum number of events per file in the temporary spooling area. Past this threshold,
     * buffered events are promoted to the final spool queue.
     * This is used in the ThresholdEventWriter (size before commits)
     *
     * @return the maximum number of events per file
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config("collector.diskspool.max-uncommitted-write-count")
    @Default("10000")
    long getMaxUncommittedWriteCount();

    /**
     * Maximum age of events in the file being written (_tmp directory).
     * <p/>
     * Maximum number of seconds before events are promoted from the temporary spooling area to the final spool queue.
     * This is used in the ThresholdEventWriter (delay between commits).
     *
     * @return maxixmum age of events in seconds in the temporary spool queue
     * @see com.ning.metrics.serialization.writer.ThresholdEventWriter
     */
    @Config("collector.diskspool.max-uncommitted-period-seconds")
    @Default("60")
    int getMaxUncommittedPeriodInSeconds();

    /**
     * Compression codec to use. Specify com.ning.metrics.collector.processing.LzfCompressionCodec
     * for lzf. Default is no compression.
     *
     * @return class to use for compressing files
     */
    @Config("collector.diskspool.compression")
    @Default("com.ning.metrics.serialization.writer.NoCompressionCodec")
    CompressionCodec getCompressionCodec();

    @Config("collector.server.ip")
    @Default("127.0.0.1")
    String getLocalIp();

    @Config("collector.server.port")
    @Default("8080")
    int getLocalPort();

    @Config("collector.server.ssl.enabled")
    @Default("false")
    boolean isSSLEnabled();

    @Config("collector.server.ssl.port")
    @Default("443")
    int getLocalSSLPort();

    @Config("collector.jetty.stats")
    @Default("true")
    boolean isJettyStatsOn();

    @Config("collector.jetty.ssl.keystore")
    @DefaultNull
    String getSSLkeystoreLocation();

    @Config("collector.jetty.ssl.keystore.password")
    @DefaultNull
    String getSSLkeystorePassword();

    @Config("collector.jetty.minThreads")
    @Default("200")
    int getJettyMinThreads();

    @Config("collector.jetty.maxThreads")
    @Default("2000")
    int getJettyMaxThreads();

    @Config("collector.jetty.LowResourcesMaxIdleTime")
    @Default("3s")
    TimeSpan getJettyLowResourcesMaxIdleTime();

    @Config("collector.jetty.maxIdleTime")
    @Default("15s")
    TimeSpan getJettyMaxIdleTime();

    @Config("collector.temporary-event-output-directory")
    @Default("/tmp/collector/hdfs/tmp")
    String getTemporaryEventOutputDirectory();

    @Config("collector.event-output-directory")
    @Default("/events")
    String getEventOutputDirectory();

    @Config("collector.hadoop-writer.max-writers")
    @Default("64")
    int getMaxHadoopWriters();

    @Config("collector.hadoop.host")
    @Default("file:///var/tmp/collector/hadoop")
    String getHfsHost();

    @Config("collector.event-end-point.enabled")
    @Default("true")
    boolean isEventEndpointEnabled();

    @Config("collector.filters.list-delimeter")
    @Default(",")
    String getFilters();

    @Config("collector.filters.host")
    @DefaultNull
    String getFiltersHost();

    @Config("collector.filters.ip")
    @DefaultNull
    String getFiltersIp();

    @Config("collector.filters.useragent")
    @DefaultNull
    String getFiltersUserAgent();

    @Config("collector.filters.path")
    @DefaultNull
    String getFiltersPath();

    @Config("collector.filters.event-type")
    @Default("CounterEvent")
    String getFiltersEventType();
    
    /**
     * If true, events specified in collector.filters.event-type 
     * will only be included else excluded
     *
     * @return whether to include or exclude the event types
     */
    @Config("collector.filters.event-type.included")
    @Default("true")
    boolean isFilteredEventTypeIncluded();

    /**
     * Default hostname to use when connecting to the load balancer
     *
     * @return the hostname used to connect to the load balancer
     */
    @Config("collector.f5.hostname")
    @DefaultNull
    String getF5Hostname();

    /**
     * Default username to use when connecting to the load balancer
     *
     * @return the username used to connect to the load balancer
     */
    @Config("collector.f5.username")
    @DefaultNull
    String getF5Username();

    /**
     * Default password to use when connecting to the load balancer
     *
     * @return the password used to connect to the load balancer
     */
    @Config("collector.f5.password")
    @DefaultNull
    String getF5Password();

    /**
     * Default pool name to use when connecting to the load balancer
     *
     * @return the pool name used to connect to the load balancer
     */
    @Config("collector.f5.poolname")
    @DefaultNull
    String getF5PoolName();

    /**
     * Goodwill hostname. This is used for the ActiveMQ integration.
     *
     * @return Goodwill hostname
     */
    @Config("collector.goodwill.host")
    @Default("127.0.0.1")
    String getGoodwillHost();

    /**
     * Goodwill port. This is used for the ActiveMQ integration.
     *
     * @return Goodwill port
     */
    @Config("collector.goodwill.port")
    @Default("8080")
    int getGoodwillPort();

    /**
     * Whether the Goodwill integration is enabled. If so, events will be sent as Json to AMQ.
     *
     * @return true if Goodwill integration is enabled, false otherwise
     */
    @Config("collector.goodwill.enabled")
    @Default("false")
    boolean isGoodwillEnabled();

    /**
     * How often to refresh the cache from Goodwill, in seconds
     *
     * @return true if Goodwill integration is enabled, false otherwise
     */
    @Config("collector.goodwill.cacheTimeout")
    @Default("90")
    int getGoodwillCacheTimeout();

    @Config("collector.nagios.enabled")
    @Default("false")
    boolean isNagiosEnabled();

    @Config("collector.nagios.host")
    @DefaultNull
    String getNagiosHost();

    @Config("collector.nagios.port")
    @Default("5667")
    int getNagiosPort();

    @Config("collector.nagios.encryption")
    @Default("NONE")
    Encryption getNagiosEncryption();

    @Config("collector.nagios.password")
    @Default("")
    String getNagiosPassword();

    @Config("collector.nagios.timeout")
    @Default("5000ms")
    TimeSpan getNagiosTimeout();

    @Config("collector.nagios.reportedHostname")
    @Default("localhost")
    String getNagiosReportedHostname();

    @Config("collector.nagios.checkRate")
    @Default("5m")
    TimeSpan getNagiosCheckRate();

    @Config("collector.nagios.serviceName")
    @Default("coll")
    String getNagiosServiceName();

    @Config("collector.shiro.configPath")
    @DefaultNull
    String getShiroConfigPath();

    @Config("collector.arecibo.enabled")
    @Default("false")
    boolean isAreciboEnabled();

    @Config("collector.arecibo.profile")
    @Default("com.ning.arecibo.jmx:name=AreciboProfile")
    String getAreciboProfile();
    
    /**
     * Comma (,) seperated class names for processing the events.
     * */
    @Config("collector.spoolWriter.classes")
    @Default("com.ning.metrics.collector.processing.db.FeedEventSpoolProcessor,com.ning.metrics.collector.processing.db.CounterEventSpoolProcessor")
    String getSpoolWriterClassNames();
    
    /**
     * Total number of threads that can run in parallel. 
     * Typically it would be 5 times the number of processor classes.
     * */
    @Config("collector.spoolWriter.fileprocessor.threads.count")
    @Default("10")
    int getFileProcessorThreadCount();
    
    /**
     * Shutdown wait time for the executor to let the existing threads finish the processing.
     * */
    @Config("collector.spoolWriter.executor.shutdown.waitTime")
    @Default("5s")
    TimeSpan getSpoolWriterExecutorShutdownTime();
    
    /**
     * Per event Flush time. THe property would be like collector.spoolWriter.eventFoo.flushtime=120s
     * */
    @Config("collector.spoolWriter.${eventName}.flushTime")
    @Default("60s")
    TimeSpan getEventFlushTime();
    
    @Config("collector.spoolWriter.db.enabled")
    @Default("true")
    boolean isSpoolWriterDbEnabled();
    
    @Description("The jdbc url for the database")
    @Config("collector.spoolWriter.jdbc.url")
    @Default("jdbc:mysql://127.0.0.1:3306/collector")
    String getJdbcUrl();
    
    @Description("The jdbc user name for the database")
    @Config("collector.spoolWriter.jdbc.user")
    @Default("root")
    String getUsername();

    @Description("The jdbc password for the database")
    @Config("collector.spoolWriter.jdbc.password")
    @Default("")
    String getPassword();

    @Description("The minimum allowed number of idle connections to the database")
    @Config("collector.spoolWriter.jdbc.minIdle")
    @Default("1")
    int getMinIdle();

    @Description("The maximum allowed number of active connections to the database")
    @Config("collector.spoolWriter.jdbc.maxActive")
    @Default("10")
    int getMaxActive();

    @Description("How long to wait before a connection attempt to the database is considered timed out")
    @Config("collector.spoolWriter.jdbc.connectionTimeout")
    @Default("10s")
    TimeSpan getConnectionTimeout();
    
    @Description("The TransactionHandler to use for all Handle instances")
    @Config("com.ning.jetty.jdbi.transactionHandler")
    @Default("com.ning.jetty.jdbi.RestartTransactionRunner")
    String getTransactionHandlerClass();
    
    @Description("How long the Subscription should be in the cache")
    @Config("collector.spoolWriter.subscription.cache.timeout")
    @Default("24h")
    TimeSpan getSubscriptionCacheTimeout();
    
    @Description("The maximum allowed number of subscriptions in the cache")
    @Config("collector.spoolWriter.subscription.cache.limit")
    @Default("1000")
    long getMaxSubscriptionCacheCount();
    
    @Description("Time period for Feed events to reside in the database before they are cleaned up")
    @Config("collector.spoolWriter.feedEvent.retention.period")
    @Default("30d")
    TimeSpan getFeedEventRetentionPeriod();
    
    @Description("How long the Counter Events should reside in cache")
    @Config("collector.spoolWriter.counterEvent.memory.flush.time")
    @Default("15m")
    TimeSpan getCounterEventMemoryFlushTime();
    
    @Description("The maximum allowed number of counter subscriptions in the cache")
    @Config("collector.spoolWriter.counterEvent.subscription.cache.limit")
    @Default("1000")
    long getMaxCounterSubscriptionCacheCount();
    
    @Description("The maximum allowed number of counter events in the cache for each subscription id before flush is done")
    @Config("collector.spoolWriter.counterEvent.cache.flush.limit")
    @Default("1000")
    long getMaxCounterEventFlushCacheCount();
    
    @Description("The maximum allowed number of counter events to be fetched in bulk while performing roll up operation")
    @Config("collector.spoolWriter.counterEvent.db.fetch.limit")
    @Default("1000")
    int getMaxCounterEventFetchCount();
    
    @Description("Cron Trigger for roll up processor execution")
    @Config("collector.spoolWriter.counterEvent.rollup.process.cron")
    @Default("0 0/30 * * * ?")
    String getCounterRollUpProcessorCronExpression();
    
    @Description("Cron Trigger for expired roll up events clean up")
    @Config("collector.spoolWriter.rollupCounterEvent.cleanup.cron")
    @Default("0 0 0 * * ?")
    String getRolledUpCounterCleanupCronExpression();
    
    @Description("How long should the rolled up counters be in the storage")
    @Config("collector.spoolWriter.rollupCounterEvent.cleanup.timeout")
    @Default("30d")
    TimeSpan getRolledUpCounterStorageTimeout();
}
