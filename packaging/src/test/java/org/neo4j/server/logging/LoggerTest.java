/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.server.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LoggerTest {

    public static Logger log = Logger.getLogger(LoggerTest.class);
    
    @Test
    public void logsShouldTolerateAccidentalControlCharacters() {
        InMemoryAppender appender = new InMemoryAppender(log);
        
        String illegalParameter = "%n";
        log.error("No configuration file at [%s]", illegalParameter);
        
        String emptyString = "";
        
        assertEquals(emptyString, appender.toString());
    }
}
