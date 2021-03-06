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

package com.novaordis.cld.keystore;

import com.novaordis.ac.Collector;
import com.novaordis.ac.CollectorFactory;
import com.novaordis.ac.Handler;
import com.novaordis.cld.KeyStore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class WriteOnlyFileKeyStore implements KeyStore
{
    // Constants -------------------------------------------------------------------------------------------------------

    // Static ----------------------------------------------------------------------------------------------------------

    // Attributes ------------------------------------------------------------------------------------------------------

    private BufferedWriter bw = null;

    private volatile boolean started;

    private Collector asyncWriter;

    private String fileName;

    // Constructors ----------------------------------------------------------------------------------------------------

    public WriteOnlyFileKeyStore(String fileName) throws Exception
    {
        this.fileName = fileName;
    }

    // Configuration implementation ------------------------------------------------------------------------------------

    @Override
    public boolean isReadOnly()
    {
        return false;
    }

    /**
     * @see com.novaordis.cld.KeyStore#store(String)
     */
    @Override
    public void store(String key) throws Exception
    {
        if (!started)
        {
            throw new IllegalArgumentException(this + " not started");
        }

        asyncWriter.handOver(key);
    }

    @Override
    public String get()
    {
        throw new IllegalStateException("this is a write-only keystore, cannot get");
    }

    @Override
    public void start() throws Exception
    {
        started = true;

        File keyFile = new File(fileName);

        bw = new BufferedWriter(new FileWriter(keyFile));

        asyncWriter = CollectorFactory.getInstance("KEY STORAGE", Thread.NORM_PRIORITY + 1);
        asyncWriter.registerHandler(new WritingHandler());
    }

    @Override
    public void stop() throws Exception
    {
        if (started)
        {
            bw.close();
            asyncWriter.dispose();
            started = false;
        }
    }

    @Override
    public boolean isStarted()
    {
        return started;
    }

    // Public ----------------------------------------------------------------------------------------------------------

    // Package protected -----------------------------------------------------------------------------------------------

    // Protected -------------------------------------------------------------------------------------------------------

    // Private ---------------------------------------------------------------------------------------------------------

    private class WritingHandler implements Handler
    {
        @Override
        public boolean canHandle(Object o)
        {
            return true;
        }

        @Override
        public void handle(long timestamp, String originatorThreadName, Object o)
        {
            String key = (String)o;

            try
            {
                bw.write(key);
                bw.newLine();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void close()
        {
        }
    }

    // Inner classes ---------------------------------------------------------------------------------------------------

}
