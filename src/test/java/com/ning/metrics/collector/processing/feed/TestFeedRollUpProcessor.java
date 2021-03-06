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
package com.ning.metrics.collector.processing.feed;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.metrics.collector.processing.db.model.Feed;
import com.ning.metrics.collector.processing.db.model.FeedEvent;
import com.ning.metrics.collector.processing.db.model.FeedEventData;
import com.ning.metrics.collector.processing.db.model.FeedEventMetaData;
import com.ning.metrics.collector.processing.db.model.RolledUpFeedEvent;
import com.ning.metrics.collector.processing.db.model.Subscription;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestFeedRollUpProcessor
{
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testFeedApplyRollUp() throws Exception{

        DateTime dt = new DateTime(DateTimeZone.UTC);

        Subscription subscription = getSubscription(1L, "topic", "channel", "feed");
        Feed feed = new Feed(Arrays.asList(getFeedEvent(subscription, "1",dt,"member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"event1\"",RolledUpEventTypes.CREATE_PHOTO.itemFieldName)));

        feed.addFeedEvents(Arrays.asList(
                                            getFeedEvent(subscription, "2",dt.plusHours(1), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"event2\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                                            getFeedEvent(subscription, "3",dt.plusHours(2), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                                            getFeedEvent(subscription, "4",dt.plusHours(25), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName)
                                        ) ,
                                        100);

        FeedRollUpProcessor feedRollUpProcessor = new FeedRollUpProcessor();

        Feed newFeed = feedRollUpProcessor.applyRollUp(feed, new HashMap<String, Object>(){{put("visibility","member");}});

        Assert.assertNotNull(newFeed);
        Assert.assertEquals(newFeed.getFeedEvents().size(), 2);
        Assert.assertEquals(newFeed.getFeedEvents().iterator().next().getClass(), RolledUpFeedEvent.class);
        Assert.assertEquals(((RolledUpFeedEvent)newFeed.getFeedEvents().iterator().next()).getCount(), 2);
    }


    @Test
    public void testFeedApplyRollUpWithSuppress() throws Exception{

        DateTime dt = new DateTime(DateTimeZone.UTC);

        Subscription subscription = getSubscription(1L, "topic", "channel", "feed");
        Feed feed = new Feed(Arrays.asList(getFeedEvent(subscription, "1",dt,"member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName)));

        feed.addFeedEvents(Arrays.asList(
                getFeedEvent(subscription, "2",dt.plusHours(1), "member1","","null,\"event2\"",""),
                getFeedEvent(subscription, "3",dt.plusHours(2), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "4",dt.plusHours(3), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",null,\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "5",dt.plusHours(4), "",FeedEventData.EVENT_TYPE_SUPPRESS,"\"mainEvent\",null,\"blahEvent\"","")
                ) ,
                100);

        FeedRollUpProcessor feedRollUpProcessor = new FeedRollUpProcessor();

        Feed newFeed = feedRollUpProcessor.applyRollUp(feed, null);

        Assert.assertNotNull(newFeed);
        Assert.assertEquals(newFeed.getFeedEvents().size(), 1);
        Assert.assertEquals(newFeed.getFeedEvents().iterator().next().getEvent().getFeedEventId(), "2");
    }

    @Test
    public void testFeedOrderWithRollup() throws Exception{
         DateTime dt = new DateTime(DateTimeZone.UTC);

        Subscription subscription = getSubscription(1L, "topic", "channel", "feed");
        Feed feed = new Feed(new ArrayList<FeedEvent>());

        feed.addFeedEvents(Arrays.asList(
                getFeedEvent(subscription, "3",dt.plusHours(3), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "1",dt.plusHours(1), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "4",dt.plusHours(4), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "5",dt.plusHours(5), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "2",dt.plusHours(2), "member1","","\"event2\"","")
                                        ) ,
                                        100);

        FeedRollUpProcessor feedRollUpProcessor = new FeedRollUpProcessor();

        Feed newFeed = feedRollUpProcessor.applyRollUp(feed, null);

        Assert.assertNotNull(newFeed);
        Assert.assertEquals(newFeed.getFeedEvents().size(), 4);

        Iterator<FeedEvent> eventIt = newFeed.getFeedEvents().iterator();

        Assert.assertEquals("5", eventIt.next().getEvent().getFeedEventId());
        Assert.assertTrue(eventIt.next() instanceof RolledUpFeedEvent);
        Assert.assertEquals("2", eventIt.next().getEvent().getFeedEventId());
        Assert.assertEquals("1", eventIt.next().getEvent().getFeedEventId());

    }


    @Test
    public void testSizeLimitRespectingRollups() throws Exception {
         DateTime dt = new DateTime(DateTimeZone.UTC);

        Subscription subscription = getSubscription(1L, "topic", "channel", "feed");
        Feed feed = new Feed(new ArrayList<FeedEvent>());

        feed.addFeedEvents(Arrays.asList(
                getFeedEvent(subscription, "3",dt.plusHours(3), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "1",dt.plusHours(1), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "4",dt.plusHours(4), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "5",dt.plusHours(5), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "2",dt.plusHours(2), "member1","","\"event2\"","")
                                        ) ,
                                        1);

        Assert.assertEquals(feed.getFeedEvents().size(), 1);

        feed = new Feed(new ArrayList<FeedEvent>());

        feed.addFeedEvents(Arrays.asList(
                getFeedEvent(subscription, "3",dt.plusHours(3), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "1",dt.plusHours(1), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "4",dt.plusHours(4), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "5",dt.plusHours(5), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "2",dt.plusHours(2), "member1","","\"event2\"","")
                                        ) ,
                                        2);

        Assert.assertEquals(feed.getFeedEvents().size(), 3);

        feed = new Feed(new ArrayList<FeedEvent>());

        feed.addFeedEvents(Arrays.asList(
                getFeedEvent(subscription, "3",dt.plusHours(3), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "1",dt.plusHours(1), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "4",dt.plusHours(4), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "5",dt.plusHours(5), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "2",dt.plusHours(2), "member1","","\"event2\"","")
                                        ) ,
                                        3);

        Assert.assertEquals(feed.getFeedEvents().size(), 4);
    }

    @Test
    public void testRollupKeepLast() throws Exception {
         DateTime dt = new DateTime(DateTimeZone.UTC);

        Subscription subscription = getSubscription(1L, "topic", "channel", "feed");
        Feed feed = new Feed(new ArrayList<FeedEvent>());

        feed.addFeedEvents(Arrays.asList(
                getFeedEvent(subscription, "3",dt.plusHours(3), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "1",dt.plusHours(1), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "4",dt.plusHours(4), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "5",dt.plusHours(5), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "2",dt.plusHours(2), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event2\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName)
                                        ) ,
                5);

        Assert.assertEquals(feed.getFeedEvents().size(), 4);
        for (FeedEvent event : feed.getFeedEvents()) {
            if (event.getEvent().getFeedEventId().equals("2")) {
                Assert.fail("rollup with id 2 should not be in rollup");
            }
        }

    }

    @Test
    public void testMinRollupKeepLast() throws Exception {
         DateTime dt = new DateTime(DateTimeZone.UTC);

        Subscription subscription = getSubscription(1L, "topic", "channel", "feed");
        Feed feed = new Feed(new ArrayList<FeedEvent>());

        feed.addFeedEvents(Arrays.asList(
                getFeedEvent(subscription, "3",dt.plusHours(3), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event3\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "1",dt.plusHours(1), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "4",dt.plusHours(4), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event4\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName),
                getFeedEvent(subscription, "5",dt.plusHours(5), "","","\"mainEvent\"",""),
                getFeedEvent(subscription, "2",dt.plusHours(2), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event2\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName)
                                        ) ,
                5);

        Assert.assertEquals(feed.getFeedEvents().size(), 4);

        for (FeedEvent event : feed.getFeedEvents()) {
            if (event.getEvent().getFeedEventId().equals("2")) {
                Assert.fail("rollup with id 2 should not be in rollup");
            }
        }

        FeedEvent lowMinRollupKeepLastEvent = getFeedEvent(subscription, "6",dt.plusHours(6), "member",RolledUpEventTypes.JOIN_GROUP.itemFieldName,"\"mainEvent\",\"event6\"",RolledUpEventTypes.JOIN_GROUP.itemFieldName);
        lowMinRollupKeepLastEvent.getEvent().setRollupKeepLast(1);

        feed.addFeedEvents(Arrays.asList(lowMinRollupKeepLastEvent), 5);

        Assert.assertEquals(feed.getFeedEvents().size(), 3);
        for (FeedEvent event : feed.getFeedEvents()) {
            if (event.getEvent().getFeedEventId().equals("2") ||
                    event.getEvent().getFeedEventId().equals("3")) {
                Assert.fail("rollup with id 2 or 3 should not be in rollup");
            }
        }


    }



    private Subscription getSubscription(Long id, String topic, String channel, String feed){
        FeedEventMetaData metadata = new FeedEventMetaData(feed);
        Subscription subscription = new Subscription(id,topic, metadata, channel);
        return subscription;
    }

    private FeedEvent getFeedEvent(Subscription subscription, String contentId, DateTime date, String visibility, String eventType, String removalTarget, String rollupKey) throws JsonParseException, JsonMappingException, IOException{
        String eventData = "{"
                + "\""+FeedEventData.FEED_EVENT_ID_KEY+"\": \""+contentId+"\","
                + "\"content-type\": \"Meal\","
                + "\"visibility\": \""+visibility+"\","
                + "\""+FeedEventData.CREATED_DATE_KEY+"\": \""+date+"\","
                + "\""+FeedEventData.EVENT_TYPE_KEY+"\": \""+eventType+"\","
                + "\""+FeedEventData.REMOVAL_TARGETS+"\": ["+removalTarget+"],"
                + "\""+FeedEventData.TOPICS_KEY+"\": [\"topic\"],"
                + "\""+FeedEventData.ROLLUP_KEY+"\": \""+rollupKey+"\","
                + "\""+FeedEventData.ROLLUP_KEEP_LAST_KEY+"\": "+2+""
         + "}";

        return new FeedEvent(mapper.readValue(eventData, FeedEventData.class),
            subscription.getChannel(),
            subscription.getId(),
            subscription.getMetadata());
    }

}
