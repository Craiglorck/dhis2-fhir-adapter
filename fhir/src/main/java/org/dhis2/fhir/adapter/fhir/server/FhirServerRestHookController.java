package org.dhis2.fhir.adapter.fhir.server;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.lang3.StringUtils;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirServerResource;
import org.dhis2.fhir.adapter.fhir.metadata.repository.FhirServerResourceRepository;
import org.dhis2.fhir.adapter.rest.RestResourceNotFoundException;
import org.dhis2.fhir.adapter.rest.RestUnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Accepts the web hook request from the server FHIR service and queues the request
 * into a queue. Before it checks if there is no such request included in the queue.
 * Not every web hook notification should result in a poll request if there is
 * already a queued poll request for the server FHIR service that handles all
 * relevant resources.
 *
 * @author volsch
 */
@RestController
@RequestMapping( "/remote-fhir-rest-hook" )
public class FhirServerRestHookController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final FhirServerResourceRepository resourceRepository;

    private final FhirServerRestHookProcessor processor;

    public FhirServerRestHookController( @Nonnull FhirServerResourceRepository resourceRepository, @Nonnull FhirServerRestHookProcessor processor )
    {
        this.resourceRepository = resourceRepository;
        this.processor = processor;
    }

    @PutMapping( path = "/{subscriptionId}/{fhirServerResourceId}/**" )
    public void receiveWithPayload( @PathVariable UUID subscriptionId, @PathVariable UUID fhirServerResourceId,
        @RequestHeader( value = "Authorization", required = false ) String authorization )
    {
        receive( subscriptionId, fhirServerResourceId, authorization );
    }

    @PostMapping( path = "/{subscriptionId}/{fhirServerResourceId}" )
    public void receive( @PathVariable UUID subscriptionId, @PathVariable UUID fhirServerResourceId,
        @RequestHeader( value = "Authorization", required = false ) String authorization )
    {
        final FhirServerResource fhirServerResource = resourceRepository.findOneByIdCached( fhirServerResourceId )
            .orElseThrow( () -> new RestResourceNotFoundException( "FHIR server data for resource cannot be found: " + fhirServerResourceId ) );
        if ( !fhirServerResource.getFhirServer().getId().equals( subscriptionId ) )
        {
            // do not give detail if the resource or the subscription cannot be found
            throw new RestResourceNotFoundException( "FHIR server data for resource cannot be found: " + fhirServerResourceId );
        }
        if ( fhirServerResource.isExpOnly() )
        {
            throw new RestResourceNotFoundException( "Subscription resource is intended for export only: " + fhirServerResourceId );
        }

        if ( StringUtils.isNotBlank( fhirServerResource.getFhirServer().getAdapterEndpoint().getAuthorizationHeader() ) &&
            !fhirServerResource.getFhirServer().getAdapterEndpoint().getAuthorizationHeader().equals( authorization ) )
        {
            throw new RestUnauthorizedException( "Authentication has failed." );
        }

        processor.process( fhirServerResource );
    }
}
