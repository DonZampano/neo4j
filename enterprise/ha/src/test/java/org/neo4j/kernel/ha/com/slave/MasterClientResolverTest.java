/**
 * Copyright (c) 2002-2015 "Neo Technology,"
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
package org.neo4j.kernel.ha.com.slave;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.cluster.client.ClusterClient;
import org.neo4j.cluster.member.ClusterMemberAvailability;
import org.neo4j.com.ComExceptionHandler;
import org.neo4j.com.IllegalProtocolVersionException;
import org.neo4j.com.storecopy.ResponseUnpacker;
import org.neo4j.kernel.ha.MasterClient210;
import org.neo4j.kernel.ha.MasterClient214;
import org.neo4j.kernel.impl.store.StoreId;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.lifecycle.LifeSupport;
import org.neo4j.kernel.logging.DevNullLoggingService;
import org.neo4j.kernel.monitoring.Monitors;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class MasterClientResolverTest
{
    @Test
    public void shouldResolveMasterClientFactory() throws Exception
    {
        // Given
        List<ComExceptionHandler> handlers = new ArrayList<>( 2 );
        MasterClientResolver resolver = newMasterClientResolver( handlers );

        LifeSupport life = new LifeSupport();
        try
        {
            life.start();
            MasterClient masterClient1 =
                    resolver.instantiate( "cluster://localhost", 44, new Monitors(), StoreId.DEFAULT, life );
            assertThat( masterClient1, instanceOf( MasterClient214.class ) );
        }
        finally
        {
            life.shutdown();
        }

        IllegalProtocolVersionException illegalProtocolVersionException = new IllegalProtocolVersionException(
                MasterClient210.PROTOCOL_VERSION.getApplicationProtocol(),
                MasterClient214.PROTOCOL_VERSION.getApplicationProtocol(),
                "Protocol is too modern" );

        // When
        for ( ComExceptionHandler handler : handlers )
        {
            handler.handle( illegalProtocolVersionException );
        }

        // Then
        life = new LifeSupport();
        try
        {
            life.start();
            MasterClient masterClient2 =
                    resolver.instantiate( "cluster://localhost", 55, new Monitors(), StoreId.DEFAULT, life );

            assertThat( masterClient2, instanceOf( MasterClient210.class ) );
        }
        finally
        {
            life.shutdown();
        }
    }

    private static MasterClientResolver newMasterClientResolver( final List<ComExceptionHandler> comExceptionHandlers )
    {
        MasterClientResolver resolver = new MasterClientResolver( new DevNullLoggingService(), StringLogger.DEV_NULL,
                mock( ResponseUnpacker.class ), mock( ClusterClient.class ), mock( ClusterMemberAvailability.class ),
                1, 1, 1, 1024 );

        MasterClientResolver resolverSpy = spy( resolver );
        doAnswer( new Answer<Void>()
        {
            @Override
            public Void answer( InvocationOnMock invocation ) throws Throwable
            {
                ComExceptionHandler handler = (ComExceptionHandler) invocation.getArguments()[1];
                comExceptionHandlers.add( handler );
                return null;
            }
        } ).when( resolverSpy ).addComExceptionHandler( any( MasterClient.class ), any( ComExceptionHandler.class ) );

        return resolverSpy;
    }
}
