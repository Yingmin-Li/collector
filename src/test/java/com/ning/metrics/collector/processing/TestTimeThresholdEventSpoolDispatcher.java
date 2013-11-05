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

package com.ning.metrics.collector.processing;

import com.ning.metrics.collector.guice.module.EventCollectorModule;
import com.ning.metrics.collector.processing.EventSpoolDispatcher;
import com.ning.metrics.collector.realtime.RealTimeQueueTestModule;
import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.thrift.ThriftEnvelope;
import com.ning.metrics.serialization.thrift.ThriftField;

import com.google.inject.Inject;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Guice(modules = {ConfigTestModule.class, EventCollectorModule.class, MockEventSpoolWriterModule.class, RealTimeQueueTestModule.class})
public class TestTimeThresholdEventSpoolDispatcher
{
    @Inject
    EventSpoolDispatcher dispatcher;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        System.setProperty("collector.spoolWriter.MockEvent.flushTime", "1s");
        dispatcher.getStats().clear();
    }

    @Test(groups = "slow")
    public void testFlushTimeThreshold() throws Exception
    {
        final List<ThriftField> fields = new ArrayList<ThriftField>();
        fields.add(ThriftField.createThriftField("hello", (short) 1));
        final ThriftEnvelope envelope = new ThriftEnvelope("MockEvent", fields);
        final Event eventA = new ThriftEnvelopeEvent(new DateTime(), envelope);

        // Send an event and wait for the dequeuer to work
        dispatcher.offer(eventA);
        
        Thread.sleep(500);
        
        Assert.assertEquals(dispatcher.getStats().getWrittenEvents(), 1);
//        Assert.assertEquals(dispatcher.getStats().getHdfsFlushes(), 0);

        // Wait for the dequeuer to work, the threshold being two in FastCollectorConfig
        Thread.sleep(2200);

        Assert.assertEquals(dispatcher.getStats().getHdfsFlushes(), 1);

        // Try again (size threshold is > 2)
        dispatcher.offer(eventA);
        Thread.sleep(2000);
        Assert.assertEquals(dispatcher.getStats().getWrittenEvents(), 2);

        Assert.assertEquals(dispatcher.getStats().getHdfsFlushes(), 2);
    }
}
