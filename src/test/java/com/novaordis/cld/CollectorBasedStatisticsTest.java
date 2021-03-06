/*
 * Copyright (c) 2015 Nova Ordis LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.novaordis.cld;

import com.novaordis.ac.Collector;
import com.novaordis.cld.mock.MockCollector;
import com.novaordis.cld.mock.MockHandler;
import com.novaordis.cld.operations.Read;
import com.novaordis.cld.operations.Write;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CollectorBasedStatisticsTest extends Assert
{
    // Constants -------------------------------------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger(CollectorBasedStatisticsTest.class);

    // Static ----------------------------------------------------------------------------------------------------------

    // Attributes ------------------------------------------------------------------------------------------------------

    // Constructors ----------------------------------------------------------------------------------------------------

    // Public ----------------------------------------------------------------------------------------------------------

    @Test
    public void recordSimpleRead() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 5L);

        Read r = new Read("a");

        s.record(0L, 10L, 20L, r, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Read r2 = new Read("b");

        s.record(2L, 10L, 20L, r2, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Read r3 = new Read("c");

        s.record(4L, 10L, 20L, r3, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Read r4 = new Read("d");

        // this goes over the sampling interval boundaries so it triggers sending a sample to the collector
        s.record(6L, 10L, 20L, r4, null);

        List<SamplingInterval> samplingIntervals = mh.getSamplingIntervals();

        assertEquals(1, samplingIntervals.size());

        SamplingInterval i = samplingIntervals.get(0);

        assertEquals(0L, i.getIntervalStartMs());
        assertEquals(3, i.getValidOperationsCount());
        assertEquals(3, i.getValidReadsCount());
        assertEquals(0, i.getValidWritesCount());
        assertEquals(30L, i.getCumulatedValidReadsTimeNano());
        assertEquals(0L, i.getCumulatedValidWritesTimeNano());

        log.debug(".");
    }

    @Test
    public void recordCombinedReadAndWrite() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 5L);

        Read r = new Read("a");

        s.record(0L, 10L, 11L, r, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Write w = new Write(0, 0, false);

        s.record(2L, 20L, 22L, w, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Read r2 = new Read("b");

        s.record(4L, 30L, 33L, r2, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Write w2 = new Write(0, 0, false);

        // this goes over the sampling interval boundaries so it triggers sending a sample to the collector
        s.record(6L, 10L, 20L, w2, null);

        List<SamplingInterval> samplingIntervals = mh.getSamplingIntervals();

        assertEquals(1, samplingIntervals.size());

        SamplingInterval i = samplingIntervals.get(0);

        assertEquals(0L, i.getIntervalStartMs());
        assertEquals(3, i.getValidOperationsCount());
        assertEquals(2, i.getValidReadsCount());
        assertEquals(1, i.getValidWritesCount());
        assertEquals(4L, i.getCumulatedValidReadsTimeNano());
        assertEquals(2L, i.getCumulatedValidWritesTimeNano());
    }

    @Test
    public void recordIntervalEdge() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 4L);

        Write w = new Write(0, 0, false);

        s.record(0L, 10L, 11L, w, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Write w2 = new Write(0, 0, false);

        s.record(2L, 20L, 22L, w2, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Write w3 = new Write(0, 0, false);

        // this is the edge of the interval
        s.record(4L, 30L, 33L, w3, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Write w4 = new Write(0, 0, false);

        // this goes over the sampling interval boundaries so it triggers sending a sample to the collector
        s.record(6L, 40L, 44L, w4, null);

        List<SamplingInterval> samplingIntervals = mh.getSamplingIntervals();

        assertEquals(1, samplingIntervals.size());

        SamplingInterval i = samplingIntervals.get(0);

        assertEquals(0L, i.getIntervalStartMs());
        assertEquals(3, i.getValidOperationsCount());
        assertEquals(0, i.getValidReadsCount());
        assertEquals(3, i.getValidWritesCount());
        assertEquals(0L, i.getCumulatedValidReadsTimeNano());
        assertEquals(6L, i.getCumulatedValidWritesTimeNano());


        samplingIntervals.clear();

        Write w5 = new Write(0, 0, false);

        // make sure the 6 ms sample is counted
        s.record(9L, 50L, 55L, w5, null);

        assertEquals(1, samplingIntervals.size());

        i = samplingIntervals.get(0);

        assertEquals(4L, i.getIntervalStartMs());
        assertEquals(1, i.getValidOperationsCount());
        assertEquals(0, i.getValidReadsCount());
        assertEquals(1, i.getValidWritesCount());
        assertEquals(0L, i.getCumulatedValidReadsTimeNano());
        assertEquals(4L, i.getCumulatedValidWritesTimeNano());
    }

    @Test
    public void recordEmptyInterval() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 4L);

        Read r = new Read("a");

        s.record(0L, 10L, 11L, r, null);

        assertTrue(mh.getSamplingIntervals().isEmpty());

        Read r2 = new Read("b");

        // this skips into the third sampling interval
        s.record(9L, 20L, 22L, r2, null);

        List<SamplingInterval> samplingIntervals = mh.getSamplingIntervals();

        assertEquals(2, samplingIntervals.size());

        SamplingInterval i = samplingIntervals.get(0);

        assertEquals(0L, i.getIntervalStartMs());
        assertEquals(1, i.getValidOperationsCount());
        assertEquals(1, i.getValidReadsCount());
        assertEquals(0, i.getValidWritesCount());
        assertEquals(1L, i.getCumulatedValidReadsTimeNano());
        assertEquals(0L, i.getCumulatedValidWritesTimeNano());

        i = samplingIntervals.get(1);

        assertEquals(4L, i.getIntervalStartMs());
        assertEquals(0, i.getValidOperationsCount());
        assertEquals(0, i.getValidReadsCount());
        assertEquals(0, i.getValidWritesCount());
        assertEquals(0L, i.getCumulatedValidReadsTimeNano());
        assertEquals(0L, i.getCumulatedValidWritesTimeNano());

        samplingIntervals.clear();

        Read r3 = new Read("c");

        // make sure the 9 ms sample is counted

        s.record(13L, 1L, 2L, r3, null);

        samplingIntervals = mh.getSamplingIntervals();

        assertEquals(1, samplingIntervals.size());

        i = samplingIntervals.get(0);

        assertEquals(8L, i.getIntervalStartMs());
        assertEquals(1, i.getValidOperationsCount());
        assertEquals(1, i.getValidReadsCount());
        assertEquals(0, i.getValidWritesCount());
        assertEquals(2L, i.getCumulatedValidReadsTimeNano());
        assertEquals(0L, i.getCumulatedValidWritesTimeNano());
    }

    // error counters --------------------------------------------------------------------------------------------------

    @Test
    public void connectionRefusedIndex_OnePerInterval() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 10L);

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        Throwable t = new java.net.ConnectException("Connection refused");

        Read r = new Read("a");

        s.record(0L, 1L, 2L, r, t);

        Read r2 = new Read("b");

        s.record(11L, -1L, -1L, r2, null);

        List<SamplingInterval> sis = mh.getSamplingIntervals();
        assertEquals(1, sis.size());

        SamplingInterval si = sis.get(0);

        long[] failureCounters = si.getFailureCounters();

        for(int i = 0; i < failureCounters.length; i ++)
        {
            if (i == RedisFailure.CONNECTION_REFUSED_INDEX)
            {
                assertEquals(1, failureCounters[i]);
            }
            else
            {
                assertEquals(0, failureCounters[i]);
            }
        }

        long[] tfc = s.getTotalFailureCounters();

        for(int i = 0; i < tfc.length; i ++)
        {
            if (i == RedisFailure.CONNECTION_REFUSED_INDEX)
            {
                assertEquals(1, failureCounters[i]);
            }
            else
            {
                assertEquals(0, failureCounters[i]);
            }
        }
    }

    @Test
    public void readTimedOut_TwoPerInterval() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 10L);


        @SuppressWarnings("ThrowableInstanceNeverThrown")
        Throwable t = new java.net.SocketTimeoutException("Read timed out");

        Read r = new Read("a");

        s.record(0L, 1L, 2L, r, t);

        Read r2 = new Read("b");

        s.record(5L, 10L, 22L, r2, t);

        Read r3 = new Read("c");

        s.record(11L, -1L, -1L, r3, null);

        List<SamplingInterval> sis = mh.getSamplingIntervals();
        assertEquals(1, sis.size());

        SamplingInterval si = sis.get(0);

        long[] failureCounters = si.getFailureCounters();

        for(int i = 0; i < failureCounters.length; i ++)
        {
            if (i == RedisFailure.READ_TIMED_OUT_INDEX)
            {
                assertEquals(2, failureCounters[i]);
            }
            else
            {
                assertEquals(0, failureCounters[i]);
            }
        }

        long[] tfc = s.getTotalFailureCounters();

        for(int i = 0; i < tfc.length; i ++)
        {
            if (i == RedisFailure.READ_TIMED_OUT_INDEX)
            {
                assertEquals(2, failureCounters[i]);
            }
            else
            {
                assertEquals(0, failureCounters[i]);
            }
        }
    }

    @Test
    public void unknownException() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 10L);

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        Throwable t = new Throwable("TEST");

        Read r = new Read("a");

        s.record(0L, 1L, 2L, r, t);

        Read r2 = new Read("b");

        s.record(11L, -1L, -1L, r2, null);

        List<SamplingInterval> sis = mh.getSamplingIntervals();
        assertEquals(1, sis.size());

        SamplingInterval si = sis.get(0);

        long[] failureCounters = si.getFailureCounters();

        for(int i = 0; i < failureCounters.length; i ++)
        {
            if (i == RedisFailure.OTHERS_INDEX)
            {
                assertEquals(1, failureCounters[i]);
            }
            else
            {
                assertEquals(0, failureCounters[i]);
            }
        }

        long[] tfc = s.getTotalFailureCounters();

        for(int i = 0; i < tfc.length; i ++)
        {
            if (i == RedisFailure.OTHERS_INDEX)
            {
                assertEquals(1, failureCounters[i]);
            }
            else
            {
                assertEquals(0, failureCounters[i]);
            }
        }
    }

    @Test
    public void combinedJedisUnknownReplyAndUnknownException() throws Exception
    {
        MockHandler mh = new MockHandler();
        Collector mc = new MockCollector(mh);

        CollectorBasedStatistics s = new CollectorBasedStatistics(mc, 10L);

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        Throwable t = new redis.clients.jedis.exceptions.JedisConnectionException("Unknown reply: something");

        Write w = new Write(0, 0, false);

        s.record(0L, 1L, 2L, w, t);

        @SuppressWarnings("ThrowableInstanceNeverThrown")
        Throwable t2 = new RuntimeException("SYNTHETIC");

        Write w2 = new Write(0, 0, false);

        s.record(5L, 3L, 4L, w2, t2);

        Write w3 = new Write(0, 0, false);

        s.record(11L, -1L, -1L, w3, null);

        List<SamplingInterval> sis = mh.getSamplingIntervals();
        assertEquals(1, sis.size());

        SamplingInterval si = sis.get(0);

        long[] failureCounters = si.getFailureCounters();

        for(int i = 0; i < failureCounters.length; i ++)
        {
            if (i == RedisFailure.JEDIS_UNKNOWN_REPLY_INDEX)
            {
                assertEquals(1, failureCounters[i]);
            }
            else if (i == RedisFailure.OTHERS_INDEX)
            {
                assertEquals(1, failureCounters[i]);
            }
            else
            {
                assertEquals(0, failureCounters[i]);
            }
        }

        long[] tfc = s.getTotalFailureCounters();

        for(int i = 0; i < tfc.length; i ++)
        {
            if (i == RedisFailure.JEDIS_UNKNOWN_REPLY_INDEX)
            {
                assertEquals(1, tfc[i]);
            }
            else if (i == RedisFailure.OTHERS_INDEX)
            {
                assertEquals(1, tfc[i]);
            }
            else
            {
                assertEquals(0, tfc[i]);
            }
        }

    }

    // Package protected -----------------------------------------------------------------------------------------------

    // Protected -------------------------------------------------------------------------------------------------------

    // Private ---------------------------------------------------------------------------------------------------------

    // Inner classes ---------------------------------------------------------------------------------------------------

}
