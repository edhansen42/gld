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

import com.novaordis.utilities.Files;
import com.novaordis.utilities.testing.Tests;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class ConfigurationImplTest extends Assert
{
    // Constants -------------------------------------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger(ConfigurationImplTest.class);

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
    public void nodes1() throws Exception
    {
        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes",
                "embedded:10001,localhost2:10002",
            });

        List<Node> nodes = c.getNodes();

        assertEquals(2, nodes.size());
        assertTrue(nodes.get(0) instanceof EmbeddedNode);
        assertEquals("localhost2", nodes.get(1).getHost());
        assertEquals(10002, nodes.get(1).getPort());
    }

    @Test
    public void nodes2() throws Exception
    {
        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes",
                "embedded:10001,",
                "localhost2:10002"
            });

        List<Node> nodes = c.getNodes();

        assertEquals(2, nodes.size());
        assertTrue(nodes.get(0) instanceof EmbeddedNode);
        assertEquals("localhost2", nodes.get(1).getHost());
        assertEquals(10002, nodes.get(1).getPort());
    }

    @Test
    public void keySizeValueSize() throws Exception
    {
        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes", "embedded",
                "--key-size", "55",
                "--value-size", "77"
            });

        assertEquals(55, c.getKeySize());
        assertEquals(77, c.getValueSize());
        assertEquals(-1L, c.getKeyExpirationSecs());
    }

    @Test
    public void expiration() throws Exception
    {
        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes", "embedded",
                "--expiration", "2"
            });

        assertEquals(2, c.getKeyExpirationSecs());
    }

    @Test
    public void maxWaitMillis() throws Exception
    {
        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes", "embedded",
            });

        assertEquals(ConfigurationImpl.DEFAULT_MAX_WAIT_MILLIS, c.getMaxWaitMillis());

        c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes", "embedded",
                "--max-wait-millis", "777"
            });

        assertEquals(777L, c.getMaxWaitMillis());
    }

    @Test
    public void noCommand() throws Exception
    {
        try
        {
            new ConfigurationImpl(new String[]
                {
                    "--nodes", "embedded"
                });

            fail("should have failed with UserErrorException, no command specified");
        }
        catch(UserErrorException e)
        {
            log.info(e.getMessage());
        }
    }

    // configuration from file -----------------------------------------------------------------------------------------

    @Test
    public void nodesFromConfigurationFile() throws Exception
    {
        File d = Tests.getScratchDirectory();
        File configurationFile = new File(d, "test.conf");

        assertTrue(Files.write(configurationFile,
            "nodes=embedded:2222\n"
        ));

        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--conf", configurationFile.getPath()
            });

        // make sure the command line overrides the configuration file value, but those that are not overridden surface

        List<Node> nodes = c.getNodes();

        assertEquals(1, nodes.size());

        Node n = nodes.get(0);

        assertTrue(n instanceof EmbeddedNode);
    }

    @Test
    public void configurationOverlay() throws Exception
    {
        File d = Tests.getScratchDirectory();
        File configurationFile = new File(d, "test.conf");

        assertTrue(Files.write(configurationFile,
            "nodes=blah:2222\n" +
            "expiration=777\n"
        ));

        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes", "embedded:10005",
                "--conf", configurationFile.getPath()
            });

        // make sure the command line overrides the configuration file value, but those that are not overridden surface

        List<Node> nodes = c.getNodes();

        assertEquals(1, nodes.size());

        Node n = nodes.get(0);

        assertTrue(n instanceof EmbeddedNode);

        assertEquals(777, c.getKeyExpirationSecs());
    }

    @Test
    public void noPassword() throws Exception
    {
        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes", "embedded",
            });

        assertNull(c.getPassword());
    }

    @Test
    public void passwordConfigFile() throws Exception
    {
        File d = Tests.getScratchDirectory();
        File configurationFile = new File(d, "test.conf");

        assertTrue(Files.write(configurationFile,
            "password=something\n"
        ));

        ConfigurationImpl c = new ConfigurationImpl(new String[]
            {
                "load",
                "--nodes", "embedded",
                "--conf", configurationFile.getPath()
            });

        assertEquals("something", c.getPassword());
    }

    @Test
    public void passwordCommandLine() throws Exception
    {
        File d = Tests.getScratchDirectory();
        File passwordFile = new File(d, ".cld.password");
        assertTrue(Files.write(passwordFile, "somethingelse\n"));

        try
        {
            System.setProperty("password.file.directory", d.getPath());

            ConfigurationImpl c = new ConfigurationImpl(new String[]
                {
                    "load",
                    "--nodes", "embedded",
                    "--password"
                });

            assertEquals("somethingelse", c.getPassword());
        }
        finally
        {
            System.clearProperty("password.file.directory");
        }
    }

    @Test
    public void passwordBothConfigFileAndCommandLine() throws Exception
    {
        File d = Tests.getScratchDirectory();
        File configurationFile = new File(d, "test.conf");
        assertTrue(Files.write(configurationFile, "password=A\n"));
        File passwordFile = new File(d, ".cld.password");
        assertTrue(Files.write(passwordFile, "commandlinetakesprecedence\n"));

        try
        {
            System.setProperty("password.file.directory", d.getPath());

            ConfigurationImpl c = new ConfigurationImpl(new String[]
                {
                    "load",
                    "--nodes", "embedded",
                    "--conf", configurationFile.getPath(),
                    "--password"
                });

            assertEquals("commandlinetakesprecedence", c.getPassword());
        }
        finally
        {
            System.clearProperty("password.file.directory");
        }
    }

    // Package protected -----------------------------------------------------------------------------------------------

    // Protected -------------------------------------------------------------------------------------------------------

    // Private ---------------------------------------------------------------------------------------------------------

    // Inner classes ---------------------------------------------------------------------------------------------------

}
