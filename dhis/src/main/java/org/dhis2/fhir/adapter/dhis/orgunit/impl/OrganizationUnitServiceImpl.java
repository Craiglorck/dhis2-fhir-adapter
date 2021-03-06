package org.dhis2.fhir.adapter.dhis.orgunit.impl;

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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.dhis2.fhir.adapter.data.model.ProcessedItemInfo;
import org.dhis2.fhir.adapter.dhis.metadata.model.DhisSyncGroup;
import org.dhis2.fhir.adapter.dhis.model.Reference;
import org.dhis2.fhir.adapter.dhis.orgunit.OrganizationUnit;
import org.dhis2.fhir.adapter.dhis.orgunit.OrganizationUnitService;
import org.dhis2.fhir.adapter.rest.RestTemplateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Implementation of {@link OrganizationUnitService}.
 *
 * @author volsch
 */
@Service
@CacheConfig( cacheNames = "organizationUnit", cacheManager = "dhisCacheManager" )
public class OrganizationUnitServiceImpl implements OrganizationUnitService
{
    protected static final String FIELDS = "lastUpdated,id,code,name,shortName,displayName,level,openingDate,closedDate,coordinates,leaf,parent[id]";

    protected static final String ORGANIZATION_UNIT_BY_CODE_URI = "/organisationUnits.json?paging=false&fields=" + FIELDS + "&filter=code:eq:{code}";

    protected static final String ORGANIZATION_UNIT_BY_NAME_URI = "/organisationUnits.json?paging=false&fields=" + FIELDS + "&filter=name:eq:{name}";

    protected static final String ORGANIZATION_UNIT_BY_ID_URI = "/organisationUnits/{id}.json?fields=" + FIELDS;

    private final RestTemplate restTemplate;

    private final ZoneId zoneId = ZoneId.systemDefault();

    @Autowired
    public OrganizationUnitServiceImpl( @Nonnull @Qualifier( "systemDhis2RestTemplate" ) RestTemplate restTemplate )
    {
        this.restTemplate = restTemplate;
    }

    @Cacheable
    @HystrixCommand
    @Override
    @Nonnull
    public Optional<OrganizationUnit> findOneByReference( @Nonnull Reference reference )
    {
        return findOneRefreshedByReference( reference );
    }

    @HystrixCommand
    @Override
    @Nonnull
    public Optional<OrganizationUnit> findOneRefreshedByReference( @Nonnull Reference reference )
    {
        final ResponseEntity<DhisOrganizationUnits> result;
        switch ( reference.getType() )
        {
            case CODE:
                result = restTemplate.getForEntity( ORGANIZATION_UNIT_BY_CODE_URI, DhisOrganizationUnits.class, reference.getValue() );
                break;
            case NAME:
                result = restTemplate.getForEntity( ORGANIZATION_UNIT_BY_NAME_URI, DhisOrganizationUnits.class, reference.getValue() );
                break;
            case ID:
                try
                {
                    return Optional.of( Objects.requireNonNull( restTemplate.getForEntity( ORGANIZATION_UNIT_BY_ID_URI, OrganizationUnit.class, reference.getValue() ).getBody() ) );
                }
                catch ( HttpClientErrorException e )
                {
                    if ( RestTemplateUtils.isNotFound( e ) )
                    {
                        return Optional.empty();
                    }
                    throw e;
                }
            default:
                throw new AssertionError( "Unhandled reference type: " + reference.getType() );
        }
        return Objects.requireNonNull( result.getBody() ).getOrganizationUnits().stream().findFirst();
    }

    @Nonnull
    @Override
    public Instant poll( @Nonnull DhisSyncGroup group, @Nonnull Instant lastUpdated, int toleranceMillis, int maxSearchCount, @Nonnull Set<String> excludedStoredBy, @Nonnull Consumer<Collection<ProcessedItemInfo>> consumer )
    {
        final OrganizationUnitPolledItemRetriever eventPolledItemRetriever = new OrganizationUnitPolledItemRetriever( restTemplate, toleranceMillis, maxSearchCount, zoneId );
        return eventPolledItemRetriever.poll( lastUpdated, excludedStoredBy, consumer, null );
    }
}
