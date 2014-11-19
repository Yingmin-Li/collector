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
package com.ning.metrics.collector.processing.db;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.ning.metrics.collector.processing.db.model.CounterEventData;
import com.ning.metrics.collector.processing.db.model.RolledUpCounter;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;

/**
 * This interface defines the methods needed to read and write to saved counter
 * information
 * @author kguthrie
 */
public interface CounterStorage
{
    public void bufferMetrics(Multimap<String, CounterEventData> dailyCounters);
    public List<CounterEventData> loadBufferedMetricsPaged(String namespace,
            DateTime toDateTime, Integer limit, Integer offset);
    public List<CounterEventData> loadBufferedMetrics(String namespace,
            DateTime toDateTime);
    public boolean deleteBufferedMetrics(Iterable<String> ids);
    public List<String> getNamespacesFromMetricsBuffer();

    public String insertOrUpdateDailyRolledUpCounter(RolledUpCounter rolledCounter);
    public RolledUpCounter loadDailyRolledUpCounter(String namespace, DateTime date);

    public List<RolledUpCounter> queryDailyRolledUpCounters(
            String namespace,
            DateTime fromDate, DateTime toDate,
            Optional<Set<String>> fetchCounterNames,
            boolean excludeDistribution,
            Optional<Integer> distributionLimit,
            Optional<Set<String>> unqiueIds);

    public int cleanExpiredDailyRolledUpCounters(DateTime toDateTime);

}
