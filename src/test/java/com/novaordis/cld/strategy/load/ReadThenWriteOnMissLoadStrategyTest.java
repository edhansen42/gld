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

package com.novaordis.cld.strategy.load;

import com.novaordis.cld.Operation;
import com.novaordis.cld.SingleThreadedRunner;
import com.novaordis.cld.SingleThreadedRunnerTest;
import com.novaordis.cld.keystore.RandomKeyGenerator;
import com.novaordis.cld.mock.MockCacheService;
import com.novaordis.cld.mock.MockConfiguration;
import com.novaordis.cld.mock.MockStatistics;
import com.novaordis.cld.operations.Read;
import com.novaordis.cld.operations.Write;
import com.novaordis.utilities.Files;
import com.novaordis.utilities.testing.Tests;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReadThenWriteOnMissLoadStrategyTest extends LoadStrategyTest
{
    // Constants -------------------------------------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger(ReadThenWriteOnMissLoadStrategyTest.class);

    // Static ----------------------------------------------------------------------------------------------------------

    // Attributes ------------------------------------------------------------------------------------------------------

    // Constructors ----------------------------------------------------------------------------------------------------

    // Public ----------------------------------------------------------------------------------------------------------

    @After
    public void scratchCleanup() throws Exception
    {
        Tests.cleanup();
    }

    @Test
    public void hit_noKeyStore() throws Exception
    {
        ReadThenWriteOnMissLoadStrategy rtwom = getLoadStrategyToTest();

        MockConfiguration mc = new MockConfiguration();
        mc.setKeySize(11);
        mc.setValueSize(17);
        mc.setUseDifferentValues(false);

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        // first operation is always a read
        Operation o = rtwom.next(null, null);

        Read r = (Read)o;

        String key = r.getKey();
        log.info(key);
        assertEquals(11, key.length());
        assertNull(r.getValue());

        // make it a "hit"
        r.setValue("something");

        o = rtwom.next(r, null);

        // the next operation is another read, for a different random key

        Read r2 = (Read)o;

        String key2 = r2.getKey();
        log.info(key2);
        assertEquals(11, key2.length());
        assertNull(r2.getValue());
        assertNotEquals(key, key2);
    }

    @Test
    public void miss_noKeyStore() throws Exception
    {
        ReadThenWriteOnMissLoadStrategy rtwom = getLoadStrategyToTest();

        MockConfiguration mc = new MockConfiguration();
        mc.setKeySize(11);
        mc.setValueSize(17);
        mc.setUseDifferentValues(false);

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        // first operation is always a read
        Operation o = rtwom.next(null, null);

        Read r = (Read)o;

        String key = r.getKey();
        log.info(key);
        assertEquals(11, key.length());

        // insure it's a miss
        assertNull(r.getValue());

        o = rtwom.next(r, null);

        // the next operation is a write for the key we missed

        Write w = (Write)o;

        String key2 = w.getKey();
        assertEquals(key, key2);

        String value = w.getValue();
        assertEquals(17, value.length());
    }

    @Test
    public void readAfterWrite_noKeyStore() throws Exception
    {
        ReadThenWriteOnMissLoadStrategy rtwom = getLoadStrategyToTest();

        MockConfiguration mc = new MockConfiguration();
        mc.setKeySize(11);
        mc.setValueSize(17);
        mc.setUseDifferentValues(false);

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        Write w = new Write(-1, -1, false);

        Operation o = rtwom.next(w, null);

        // the next operation after a write is another read

        Read r = (Read)o;

        String key = r.getKey();
        log.info(key);
        assertEquals(11, key.length());
    }

    @Test
    public void hit_validKeyStore() throws Exception
    {
        File keyStoreFile = new File(Tests.getScratchDir(), "keys.txt");
        Files.write(keyStoreFile, "KEY0\nKEY1\nKEY2\n");

        ReadThenWriteOnMissLoadStrategy rtwom = getLoadStrategyToTest();

        MockConfiguration mc = new MockConfiguration();
        mc.setKeySize(11);
        mc.setValueSize(17);
        mc.setUseDifferentValues(false);
        mc.setKeyStoreFile(keyStoreFile.getPath());

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        // first operation is always a read
        Operation o = rtwom.next(null, null);

        Read r = (Read)o;

        String key = r.getKey();
        log.info(key);
        assertEquals("KEY0", key);
        assertNull(r.getValue());

        // make it a "hit"
        r.setValue("something");

        o = rtwom.next(r, null);

        // the next operation is another read, for the next key

        Read r2 = (Read)o;

        String key2 = r2.getKey();
        log.info(key2);
        assertEquals("KEY1", key2);
        assertNull(r2.getValue());
    }

    @Test
    public void miss_validKeyStore() throws Exception
    {
        File keyStoreFile = new File(Tests.getScratchDir(), "keys.txt");
        Files.write(keyStoreFile, "KEY0\nKEY1\nKEY2\n");

        ReadThenWriteOnMissLoadStrategy rtwom = getLoadStrategyToTest();

        MockConfiguration mc = new MockConfiguration();
        mc.setKeySize(11);
        mc.setValueSize(17);
        mc.setUseDifferentValues(false);
        mc.setKeyStoreFile(keyStoreFile.getPath());

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        // first operation is always a read
        Operation o = rtwom.next(null, null);

        Read r = (Read)o;

        String key = r.getKey();
        log.info(key);
        assertEquals("KEY0", key);

        // insure it's a miss
        assertNull(r.getValue());

        o = rtwom.next(r, null);

        // the next operation is a write for the key we missed

        Write w = (Write)o;

        String key2 = w.getKey();
        assertEquals(key, key2);

        String value = w.getValue();
        assertEquals(17, value.length());
    }

    @Test
    public void readAfterWrite_validKeyStore() throws Exception
    {
        File keyStoreFile = new File(Tests.getScratchDir(), "keys.txt");
        Files.write(keyStoreFile, "KEY0\nKEY1\nKEY2\n");

        ReadThenWriteOnMissLoadStrategy rtwom = getLoadStrategyToTest();

        MockConfiguration mc = new MockConfiguration();
        mc.setKeySize(11);
        mc.setValueSize(17);
        mc.setUseDifferentValues(false);
        mc.setKeyStoreFile(keyStoreFile.getPath());

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        Write w = new Write(-1, -1, false);

        Operation o = rtwom.next(w, null);

        // the next operation after a write is another read

        Read r = (Read)o;

        String key = r.getKey();
        log.info(key);
        assertEquals("KEY0", key);
    }

    //
    // integration with SingleThreadedRunner
    //

    @Test
    public void integration_ReadThenWriteOnMiss_SingleThreadedRunner_OneOperation() throws Exception
    {
        MockCacheService mcs = new MockCacheService();

        MockConfiguration mc = new MockConfiguration();
        mc.setCacheService(mcs);

        MockStatistics ms = new MockStatistics(false);
        ms.setDoneAfterNRecords(1);

        CyclicBarrier barrier = new CyclicBarrier(1);

        ReadThenWriteOnMissLoadStrategy rtwom = new ReadThenWriteOnMissLoadStrategy();

        mc.setKeySize(1);
        mc.setValueSize(1);
        mc.setUseDifferentValues(false);

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        assertTrue(rtwom.getKeyStore() instanceof RandomKeyGenerator);

        SingleThreadedRunner st = new SingleThreadedRunner("TEST", mc, rtwom, ms, barrier);
        SingleThreadedRunnerTest.setRunning(st);

        st.run();

        List<MockStatistics.OperationThrowablePair> recorded = ms.getRecorded();
        assertEquals(1, recorded.size());

        Read r = (Read)recorded.get(0).operation;
        assertNull(recorded.get(0).throwable);

        assertTrue(r.hasBeenPerformed());
        assertNull(r.getValue());
    }

    @Test
    public void integration_ReadThenWriteOnMiss_SingleThreadedRunner_ReadThenWrite() throws Exception
    {
        MockCacheService mcs = new MockCacheService();

        MockConfiguration mc = new MockConfiguration();
        mc.setCacheService(mcs);

        MockStatistics ms = new MockStatistics(false);
        ms.setDoneAfterNRecords(2);

        CyclicBarrier barrier = new CyclicBarrier(1);

        ReadThenWriteOnMissLoadStrategy rtwom = new ReadThenWriteOnMissLoadStrategy();

        mc.setKeySize(1);
        mc.setValueSize(1);
        mc.setUseDifferentValues(false);

        rtwom.configure(mc, Collections.<String>emptyList(), 0);

        assertTrue(rtwom.getKeyStore() instanceof RandomKeyGenerator);

        SingleThreadedRunner st = new SingleThreadedRunner("TEST", mc, rtwom, ms, barrier);
        SingleThreadedRunnerTest.setRunning(st);

        st.run();

        List<MockStatistics.OperationThrowablePair> recorded = ms.getRecorded();
        assertEquals(2, recorded.size());

        Read r = (Read)recorded.get(0).operation;
        Throwable t = recorded.get(0).throwable;

        assertTrue(r.hasBeenPerformed());
        assertNull(r.getValue());
        String key = r.getKey();
        log.info("key=" + key);
        assertNull(t);

        Write w = (Write)recorded.get(1).operation;
        Throwable t2 = recorded.get(1).throwable;

        assertEquals(key, w.getKey());
        String value = w.getValue();
        log.info("key=" + key);
        assertNull(t2);


        //
        // make sure the key was written in cache
        //

        assertEquals(value, mcs.get(key));
    }

    // Package protected -----------------------------------------------------------------------------------------------

    // Protected -------------------------------------------------------------------------------------------------------

    @Override
    protected ReadThenWriteOnMissLoadStrategy getLoadStrategyToTest() throws Exception
    {
        return new ReadThenWriteOnMissLoadStrategy();
    }

    // Private ---------------------------------------------------------------------------------------------------------

    // Inner classes ---------------------------------------------------------------------------------------------------

}
