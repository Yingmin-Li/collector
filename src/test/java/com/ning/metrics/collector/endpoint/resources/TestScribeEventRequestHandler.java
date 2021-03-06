/*
 * Copyright 2010 Ning, Inc.
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

package com.ning.metrics.collector.endpoint.resources;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;

import java.util.ArrayList;
import java.util.List;

public class TestScribeEventRequestHandler
{
    private MockScribeEventHandler eventHandler = null;
    private ScribeEventRequestHandler eventRequestHandler = null;

    private static final String EVENT_NAME = "myMsg";
    private static final DateTime EVENT_DATE_TIME = new DateTime();
    private final String THRIFT_MSG = String.format("%s:msg", EVENT_DATE_TIME.getMillis());

    @BeforeMethod(alwaysRun = true)
    public void setup()
    {
        eventHandler = new MockScribeEventHandler();
        eventRequestHandler = new ScribeEventRequestHandler(eventHandler);
    }

    @Test(groups = "fast")
    public void testSuccess() throws Exception
    {
        final List<LogEntry> logEntries = new ArrayList<LogEntry>();
        logEntries.add(new LogEntry(EVENT_NAME, THRIFT_MSG));
        Assert.assertEquals(eventRequestHandler.Log(logEntries), ResultCode.OK);

        Assert.assertEquals(eventHandler.getProcessedEventList().size(), 1);
        Assert.assertEquals(eventHandler.getProcessedEventList().get(0).getName(), EVENT_NAME);
    }

    @Test(groups = "fast")
    public void testUnsupportedEvent() throws Exception
    {
        final List<LogEntry> logEntries = new ArrayList<LogEntry>();
        logEntries.add(new LogEntry(EVENT_NAME, "msg"));
        Assert.assertEquals(eventRequestHandler.Log(logEntries), ResultCode.OK);

        Assert.assertEquals(eventHandler.getProcessedEventList().size(), 0);
    }

    @Test(groups = "fast")
    public void testCollectorFailure() throws Exception
    {
        eventHandler.setFakeCollectorFailure(true);
        final List<LogEntry> logEntries = new ArrayList<LogEntry>();
        logEntries.add(new LogEntry(EVENT_NAME, null));
        Assert.assertEquals(eventRequestHandler.Log(logEntries), ResultCode.OK);

        Assert.assertEquals(eventHandler.isHandleFailureCalled(), true);
        Assert.assertEquals(eventHandler.getProcessedEventList().size(), 0);
    }
}
