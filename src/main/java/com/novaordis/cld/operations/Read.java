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

package com.novaordis.cld.operations;

import com.novaordis.cld.Operation;
import com.novaordis.cld.CacheService;

public class Read implements Operation
{
    // Constants -------------------------------------------------------------------------------------------------------

    // Static ----------------------------------------------------------------------------------------------------------

    // Attributes ------------------------------------------------------------------------------------------------------

    private String key;
    private String value;
    private volatile boolean performed;

    // Constructors ----------------------------------------------------------------------------------------------------

    public Read(String key)
    {
        this.key = key;
    }

    // Operation implementation ----------------------------------------------------------------------------------------

    /**
     * @see Operation#perform(com.novaordis.cld.CacheService)
     */
    @Override
    public void perform(CacheService rs) throws Exception
    {
        performed = true;
        value = rs.get(key);
    }

    // Public ----------------------------------------------------------------------------------------------------------

    /**
     * May return null in case of cache miss.
     */
    public String getValue()
    {
        return value;
    }

    public String getKey()
    {
        return key;
    }

    public void setValue(String s)
    {
        this.value = s;
    }

    public boolean hasBeenPerformed()
    {
        return performed;
    }

    @Override
    public String toString()
    {
        return key + (!performed ? "" : (value == null ? " miss" : " hit (" + value + ")"));
    }

    // Package protected -----------------------------------------------------------------------------------------------

    // Protected -------------------------------------------------------------------------------------------------------

    // Private ---------------------------------------------------------------------------------------------------------

    // Inner classes ---------------------------------------------------------------------------------------------------

}
