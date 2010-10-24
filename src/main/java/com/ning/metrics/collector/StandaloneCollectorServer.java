/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.metrics.collector;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.ServletModule;
import com.ning.metrics.collector.endpoint.servers.JettyServer;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.apache.log4j.Logger;

import com.ning.metrics.collector.binder.modules.EventCollectorModule;
import com.ning.metrics.collector.binder.modules.OpenSourceCollectorModule;
import com.ning.metrics.collector.binder.modules.ScribeModule;
import com.ning.metrics.collector.endpoint.servers.ScribeServer;

import java.util.HashMap;
import java.util.Map;

/**
 * If you are writing your own Main class, make sure to match the name since
 * the GuiceServletContextListener implementation needs to access the injector created here.
 *
 * @see com.ning.metrics.collector.binder.modules.JettyListener:getInjector
 */
public class StandaloneCollectorServer
{
    private final static Logger log = Logger.getLogger(StandaloneCollectorServer.class);
    private static Injector injector = null;

    public static void main(String... args) throws Exception
    {
        final long startTime = System.currentTimeMillis();

        /* Scan for Jersey endpoints */
        final Map<String, String> params = new HashMap<String, String>();
        params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.ning.metrics.collector.endpoint");

        injector = Guice.createInjector(
            new EventCollectorModule(),      /* Required, wire up the event processor and the writers */
            new OpenSourceCollectorModule(), /* Open-Source version of certain interfaces */

            new ScribeModule(),              /* Optional, provide the Scribe endpoint */

            new ServletModule()              /* Optional, provide the Jetty endpoint */
            {
                @Override
                protected void configureServlets()
                {
                    // Note! It's "*", NOT "/*"
                    serve("*").with(GuiceContainer.class, params);
                }
            }
        );

        /* Start the Jetty endpoint */
        injector.getInstance(JettyServer.class);

        /* Start the Scribe endpoint */
        injector.getInstance(ScribeServer.class);

        final long secondsToStart = (System.currentTimeMillis() - startTime) / 1000;
        log.info(String.format("Collector initialized in %d:%02d", secondsToStart / 60, secondsToStart % 60));
    }

    /**
     * Hack to share the injector with the Jersey GuiceFilter
     *
     * @see com.ning.metrics.collector.binder.modules.JettyListener:getInjector
     */
    public static Injector getInjector()
    {
        return injector;
    }
}