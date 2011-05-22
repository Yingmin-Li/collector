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

package com.ning.metrics.collector.hadoop.processing;

import com.google.inject.Inject;
import com.ning.metrics.collector.MockEvent;
import com.ning.metrics.collector.binder.annotations.HdfsDiskSpoolFlushExecutor;
import com.ning.metrics.collector.endpoint.EventStats;
import com.ning.metrics.collector.realtime.EventQueueProcessor;
import com.ning.metrics.serialization.writer.DiskSpoolEventWriter;
import com.ning.metrics.serialization.writer.EventWriter;
import com.ning.metrics.serialization.writer.MockEventWriter;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.concurrent.ScheduledExecutorService;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test(groups = {"fast"})
@Guice(modules = MockCollectorModule.class)
public class TestEventCollectorProvider
{
    @Inject
    private BufferingEventCollector collector;

    @Inject
    private EventQueueProcessor queueProcessor;

    @Inject
    private MockEventWriter bufferingEventWriter;

    @Inject
    private EventSpoolDispatcher dispatcher;

    @Inject
    @HdfsDiskSpoolFlushExecutor
    private ScheduledExecutorService hdfsExecutor;

    @Inject
    private EventWriter hdfsEventWriter;

    @Inject
    private DiskSpoolEventWriter hdfsWriter;

    @Test
    public void testGet() throws Exception
    {
        assertTrue(collector.collectEvent(new MockEvent(), new EventStats()));
        assertEquals(bufferingEventWriter.getCommittedEventList().size(), 0);
        assertEquals(((MockEventWriter) hdfsEventWriter).getCommittedEventList().size(), 0);

        EventCollectorProvider.mainCollectorShutdownHook(collector, dispatcher);

        assertFalse(collector.collectEvent(new MockEvent(), new EventStats()));

        // AMQ should be down
        assertFalse(queueProcessor.isRunning());

        // Writer to disk should be down
        assertFalse(dispatcher.isRunning());


        assertEquals(bufferingEventWriter.getCommittedEventList().size(), 1);

        // TODO Test queues and workers are down

        assertTrue(hdfsExecutor.isTerminated()); // Writer executor

        // TODO
        //assertEquals(((MockEventWriter) hdfsEventWriter).getFlushedEventList().size(), 1);
    }
}
