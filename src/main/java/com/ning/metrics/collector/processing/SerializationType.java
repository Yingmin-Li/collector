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

import com.ning.metrics.serialization.event.Event;
import com.ning.metrics.serialization.event.EventSerializer;
import com.ning.metrics.serialization.event.SmileEnvelopeEvent;
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;
import com.ning.metrics.serialization.smile.SmileEnvelopeEventSerializer;
import com.ning.metrics.serialization.thrift.ThriftEnvelopeEventSerializer;
import com.ning.metrics.serialization.writer.ObjectOutputEventSerializer;

public enum SerializationType
{
    SMILE("smile")
        {
            @Override
            public EventSerializer getSerializer()
            {
                return new SmileEnvelopeEventSerializer(false);
            }
        },
    JSON("json")
        {
            @Override
            public EventSerializer getSerializer()
            {
                return new SmileEnvelopeEventSerializer(true);
            }
        },
    THRIFT("thrift")
        {
            @Override
            public EventSerializer getSerializer()
            {
                return new ThriftEnvelopeEventSerializer();
            }
        },
    DEFAULT("bin")
        {
            @Override
            public EventSerializer getSerializer()
            {
                // TODO since we want to stop using ObjectOutput altogether, should we instead use ThriftEnvelopeEventSerializer?
                return new ObjectOutputEventSerializer();
            }
        };

    private final String suffix;

    private SerializationType(String suffix)
    {
        this.suffix = suffix;
    }

    public abstract EventSerializer getSerializer();

    public static SerializationType get(final Event event)
    {
        if (event instanceof SmileEnvelopeEvent) {
            if (((SmileEnvelopeEvent) event).isPlainJson()) {
                return JSON;
            }
            else {
                return SMILE;
            }
        }
        else if (event instanceof ThriftEnvelopeEvent) {
            return THRIFT;
        }
        else {
            return DEFAULT;
        }
    }

    public String getFileSuffix()
    {
        return suffix;
    }

    public static SerializationType fromSuffix(String suffix)
    {
        if (suffix.equals("smile")) {
            return SMILE;
        }
        else if (suffix.equals("json")) {
            return JSON;
        }
        else if (suffix.equals("thrift")) {
            return THRIFT;
        }
        else if (suffix.equals("bin")) {
            return DEFAULT;
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
