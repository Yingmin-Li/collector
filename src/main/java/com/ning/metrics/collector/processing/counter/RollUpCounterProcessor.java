/*
 * Copyright 2010-2014 Ning, Inc.
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
package com.ning.metrics.collector.processing.counter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;
import com.ning.metrics.collector.binder.config.CollectorConfig;
import com.ning.metrics.collector.processing.db.DatabaseCounterStorage;
import com.ning.metrics.collector.processing.db.DatabaseCounterStorage.CounterEventDataMapper;
import com.ning.metrics.collector.processing.db.model.CounterEventData;
import com.ning.metrics.collector.processing.db.model.CounterSubscription;
import com.ning.metrics.collector.processing.db.model.RolledUpCounter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.ResultIterator;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class RollUpCounterProcessor
{
    private static final Logger log = LoggerFactory.getLogger(RollUpCounterProcessor.class);
    private final IDBI dbi;
    private final CollectorConfig config;
    private final DatabaseCounterStorage counterStorage;
    private final ObjectMapper mapper;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final static Ordering<RolledUpCounter> orderingRolledUpCounterByDate = new Ordering<RolledUpCounter>() {

        @Override
        public int compare(RolledUpCounter left, RolledUpCounter right)
        {
            return left.getFromDate().compareTo(right.getFromDate());
        }};
        
    @Inject
    public RollUpCounterProcessor(final IDBI dbi, final DatabaseCounterStorage counterStorage, final CollectorConfig config, ObjectMapper mapper)
    {
        this.dbi = dbi;
        this.counterStorage = counterStorage;
        this.config = config;
        this.mapper = mapper;
    }
    
    public void rollUpStreamingDailyCounters(final CounterSubscription counterSubscription)
    {
        try {
            if (!isProcessing.compareAndSet(false, true)) {
                log.info("Asked to do counter roll up, but we're already processing!");
                return;
            }
            
            log.info(String.format("Running roll up process for Counter Subscription [%s]", counterSubscription.getAppId()));
            
            final DateTime toDateTime = new DateTime(DateTimeZone.UTC);
            Map<String, RolledUpCounter> rolledUpCounterMap = streamAndProcessDailyCounterData(counterSubscription,toDateTime);
            
            postRollUpProcess(counterSubscription, toDateTime, rolledUpCounterMap);
            
            log.info(String.format("Roll up process for Counter Subscription [%s] completed successfully!", counterSubscription.getAppId()));
        }
        catch (Exception e) {
            log.error(String.format("Exception occurred while performing counter roll up for [%s]", counterSubscription.getAppId()),e);
        }
        finally{
            isProcessing.set(false);
        }
        
    }

    private Map<String, RolledUpCounter> streamAndProcessDailyCounterData(final CounterSubscription counterSubscription,final DateTime toDateTime)
    {
        return dbi.withHandle(new HandleCallback<Map<String,RolledUpCounter>>() {

            @Override
            public Map<String, RolledUpCounter> withHandle(Handle handle) throws Exception
            {
                final String queryStr = "select metrics from metrics_daily where subscription_id = :subscriptionId" 
                        +" and created_date <= :toDateTime";
                
                Query<Map<String, Object>> query = handle.createQuery(queryStr)
                        .bind("subscriptionId", counterSubscription.getId())
                        .setFetchSize(Integer.MIN_VALUE);
                
                query.bind("toDateTime", DatabaseCounterStorage.DAILY_METRICS_STORAGE_DATE_FORMATER.print(toDateTime));
                
                Map<String,RolledUpCounter> rolledUpCounterMap = new ConcurrentHashMap<String, RolledUpCounter>();
                
                ResultIterator<CounterEventData> streamingIterator = null;
                
                try {
                    streamingIterator = query.map(new CounterEventDataMapper(mapper)).iterator();
                    
                    if(Objects.equal(null, streamingIterator))
                    {
                        return rolledUpCounterMap;
                    }
                    
                    while(streamingIterator.hasNext())
                    {
                        processCounterEventData(counterSubscription, rolledUpCounterMap, streamingIterator.next());
                    }
                }
                catch (Exception e) {
                    log.error(String.format("Exception occurred while streaming and rolling up daily counter for app id: %s", counterSubscription.getAppId()), e);
                }
                finally {
                    if (streamingIterator != null) {
                        streamingIterator.close();
                    }
                }
                
                return rolledUpCounterMap;
            }});
    }
    
    public void rollUpDailyCounters(final CounterSubscription counterSubscription){
        try {
            if (!isProcessing.compareAndSet(false, true)) {
                log.info("Asked to do counter roll up, but we're already processing!");
                return;
            }
            final DateTime toDateTime = new DateTime(DateTimeZone.UTC);
            final Integer recordFetchLimit = config.getMaxCounterEventFetchCount();
            Integer recordOffSetCounter = 0;
            boolean fetchMoreRecords = true;
            
            Map<String, RolledUpCounter> rolledUpCounterMap = new ConcurrentHashMap<String, RolledUpCounter>();
            
            log.info(String.format("Running roll up process for Counter Subscription [%s]", counterSubscription.getAppId()));
            
            while(fetchMoreRecords)
            {
             // Load daily counters stored for the respective subscription limiting to now() and getMaxCounterEventFetchCount
                Iterator<CounterEventData> dailyCounterList = counterStorage.loadDailyMetrics(counterSubscription.getId(), toDateTime, recordFetchLimit, recordOffSetCounter).iterator();
                if(Objects.equal(null, dailyCounterList) || !dailyCounterList.hasNext())
                {
                    fetchMoreRecords = false;
                    break;
                }
                
                log.info(String.format("Processing counter events for %s on offset %d", counterSubscription.getAppId(), recordOffSetCounter));
                
                // increment recordOffset to fetch next getMaxCounterEventFetchCount
                recordOffSetCounter += recordFetchLimit;
                
                while(dailyCounterList.hasNext())
                {
                    processCounterEventData(counterSubscription, rolledUpCounterMap, dailyCounterList.next());
                }
                
                log.info(String.format("Roll up completed %s on offset %d", counterSubscription.getAppId(), recordOffSetCounter));
                
            }
            
            postRollUpProcess(counterSubscription, toDateTime, rolledUpCounterMap);
            
            log.info(String.format("Roll up process for Counter Subscription [%s] completed successfully!", counterSubscription.getAppId()));
        }
        catch (Exception e) {
            log.error(String.format("Exception occurred while performing counter roll up for [%s]", counterSubscription.getAppId()),e);
        }
        finally{
            isProcessing.set(false);
        }
        
    }

    private void postRollUpProcess(final CounterSubscription counterSubscription, final DateTime toDateTime, Map<String, RolledUpCounter> rolledUpCounterMap)
    {
        if(!rolledUpCounterMap.isEmpty())
        {
            log.info(String.format("Evaluating Uniques and updating roll up counter for %s", counterSubscription.getAppId()));
            for(RolledUpCounter rolledUpCounter : rolledUpCounterMap.values())
            {
                // Evaluate Uniqes for rolled up counters
                rolledUpCounter.evaluateUniques();
                
                //Save
                counterStorage.insertOrUpdateRolledUpCounter(counterSubscription.getId(), rolledUpCounter);
            }
            
            log.info(String.format("Deleting daily counters for %s which are <= %s", counterSubscription.getAppId(), toDateTime));
            // Delete daily metrics which have been accounted for the roll up. 
            // There may be more additions done since this process started which is why the evaluation time is passed on.
            counterStorage.deleteDailyMetrics(counterSubscription.getId(), toDateTime);
        }
    }

    private void processCounterEventData(final CounterSubscription counterSubscription, Map<String, RolledUpCounter> rolledUpCounterMap, final CounterEventData counterEventData)
    {
        final String rolledUpCounterKey = counterSubscription.getAppId()+counterEventData.getFormattedDate();
        
        RolledUpCounter rolledUpCounter = rolledUpCounterMap.get(rolledUpCounterKey);
        
        if(Objects.equal(null, rolledUpCounter))
        {
            rolledUpCounter = counterStorage.loadRolledUpCounterById(rolledUpCounterKey, false, Optional.of(0));
            if(Objects.equal(null, rolledUpCounter))
            {
                rolledUpCounter = new RolledUpCounter(counterSubscription.getAppId(), counterEventData.getCreatedDate(), counterEventData.getCreatedDate());
            }
        }
        
        rolledUpCounter.updateRolledUpCounterData(counterEventData, counterSubscription.getIdentifierDistribution().get(counterEventData.getIdentifierCategory()));
        rolledUpCounterMap.put(rolledUpCounterKey, rolledUpCounter);
    }
    
    public List<RolledUpCounter> loadAggregatedRolledUpCounters(final String appId, final Optional<String> fromDateOpt, final Optional<String> toDateOpt, final Optional<Set<String>> counterTypesOpt, final boolean aggregateByMonth, final boolean excludeDistribution, final Optional<Integer> distributionLimit)
    {
        CounterSubscription counterSubscription = counterStorage.loadCounterSubscription(appId);
        if(counterSubscription == null)
        {   
            return Lists.newArrayList();
        }
        DateTime fromDate = fromDateOpt.isPresent()?new DateTime(RolledUpCounter.ROLLUP_COUNTER_DATE_FORMATTER.parseMillis(fromDateOpt.get()),DateTimeZone.UTC):null;
        DateTime toDate = toDateOpt.isPresent()?new DateTime(RolledUpCounter.ROLLUP_COUNTER_DATE_FORMATTER.parseMillis(toDateOpt.get()),DateTimeZone.UTC):null;
        
        List<RolledUpCounter> rolledUpCounterResult = counterStorage.loadRolledUpCounters(counterSubscription.getId(), fromDate, toDate, counterTypesOpt, excludeDistribution, distributionLimit);
        
        if(Objects.equal(null, rolledUpCounterResult) || rolledUpCounterResult.size() == 0)
        {
            return Lists.newArrayList();
        }
        
        if(aggregateByMonth)
        {
            // TODO - Write the routine to aggregate the data by month
        }
            
        return orderingRolledUpCounterByDate.immutableSortedCopy(rolledUpCounterResult);
    }

}
