/*
 * Copyright 2010-2012 Ning, Inc.
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

package com.ning.metrics.collector.filtering;

import com.ning.metrics.collector.endpoint.ParsedRequest;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

public class TestOrFilter
{
    private static final Filter<ParsedRequest> TRUE_FILTER = new Filter<ParsedRequest>()
    {
        @Override
        public boolean passesFilter(final String eventName, final ParsedRequest request)
        {
            return true;
        }
    };

    private static final Filter<ParsedRequest> FALSE_FILTER = new Filter<ParsedRequest>()
    {
        @Override
        public boolean passesFilter(final String eventName, final ParsedRequest request)
        {
            return false;
        }
    };

    @Test(groups = "fast")
    public void testEmptySetFilterFilter() throws Exception
    {
        final Filter<ParsedRequest> filter = new OrFilter(Collections.<Filter<ParsedRequest>>emptyList());
        Assert.assertEquals(filter.passesFilter(null, null), false);
    }

    @Test(groups = "fast")
    public void testSingleFilter() throws Exception
    {
        final Filter<ParsedRequest> trueFilter = new OrFilter(Arrays.asList(TRUE_FILTER));
        Assert.assertEquals(trueFilter.passesFilter(null, null), true);

        final Filter<ParsedRequest> falseFilter = new OrFilter(Arrays.asList(FALSE_FILTER));
        Assert.assertEquals(falseFilter.passesFilter(null, null), false);
    }

    @Test(groups = "fast")
    public void testMultipleFilters() throws Exception
    {
        final Filter<ParsedRequest> trueFilter = new OrFilter(Arrays.asList(TRUE_FILTER, FALSE_FILTER));
        Assert.assertEquals(trueFilter.passesFilter(null, null), true);

        final Filter<ParsedRequest> falseFilter = new OrFilter(Arrays.asList(FALSE_FILTER, FALSE_FILTER));
        Assert.assertEquals(falseFilter.passesFilter(null, null), false);
    }
}