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

package com.ning.metrics.collector.endpoint.extractors;

import com.google.inject.Inject;
import com.ning.metrics.collector.endpoint.ExtractedAnnotation;
import com.ning.metrics.collector.events.parsing.EventParser;
import com.ning.metrics.serialization.event.Event;
import org.apache.log4j.Logger;
import org.weakref.jmx.Managed;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API versions 1 and 2: query parameters-based API (via GET).
 * The lower level extraction business happens in EventParser.
 * <p/>
 * The class needs to be public for JMX.
 *
 * @see EventParser
 */
public class QueryParameterEventExtractor implements EventExtractor
{
    private static final Logger log = Logger.getLogger(QueryParameterEventExtractor.class);

    private final EventParser thriftEventParser;
    private final AtomicLong thriftSuccess = new AtomicLong(0);
    private final AtomicLong thriftFailure = new AtomicLong(0);

    @Inject
    public QueryParameterEventExtractor(final EventParser thriftEventParser)
    {
        this.thriftEventParser = thriftEventParser;
    }

    @Override
    public Collection<? extends Event> extractEvent(final ExtractedAnnotation annotation) throws EventParsingException
    {
        final String eventName = annotation.getEventName();

        if (eventName != null) {
            log.debug(String.format("Query parameter to process: %s", eventName));
            final String type = eventName.substring(0, eventName.indexOf(","));
            final String eventTypeString = eventName.substring(eventName.indexOf(",") + 1);

            log.debug(String.format("Event type [%s], event string [%s]", type, eventTypeString));

            // This API only supports sending one event at a time
            try {
                final Event event = thriftEventParser.parseThriftEvent(type, eventTypeString, annotation);
                thriftSuccess.incrementAndGet();

                return Collections.singletonList(event);
            }
            catch (EventParsingException e) {
                thriftFailure.incrementAndGet();
                throw e;
            }
        }
        else {
            throw new EventParsingException("Event name not specified");
        }
    }

    @Managed(description = "Number of Thrift events the collector successfully deserialized")
    public long getThriftSuccess()
    {
        return thriftSuccess.get();
    }

    @Managed(description = "Number of Thrift events the collector couldn't deserialize")
    public long getThriftFailure()
    {
        return thriftFailure.get();
    }
}
