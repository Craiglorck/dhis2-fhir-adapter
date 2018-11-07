package org.dhis2.fhir.adapter.setup;

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
import org.dhis2.fhir.adapter.fhir.metadata.model.AuthenticationMethod;
import org.dhis2.fhir.adapter.fhir.metadata.model.Code;
import org.dhis2.fhir.adapter.fhir.metadata.model.CodeCategory;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirResourceType;
import org.dhis2.fhir.adapter.fhir.metadata.model.RemoteSubscription;
import org.dhis2.fhir.adapter.fhir.metadata.model.RemoteSubscriptionSystem;
import org.dhis2.fhir.adapter.fhir.metadata.model.RequestHeader;
import org.dhis2.fhir.adapter.fhir.metadata.model.SubscriptionAdapterEndpoint;
import org.dhis2.fhir.adapter.fhir.metadata.model.SubscriptionDhisEndpoint;
import org.dhis2.fhir.adapter.fhir.metadata.model.SubscriptionFhirEndpoint;
import org.dhis2.fhir.adapter.fhir.metadata.model.System;
import org.dhis2.fhir.adapter.fhir.metadata.model.SystemCode;
import org.dhis2.fhir.adapter.fhir.metadata.repository.CodeCategoryRepository;
import org.dhis2.fhir.adapter.fhir.metadata.repository.CodeRepository;
import org.dhis2.fhir.adapter.fhir.metadata.repository.RemoteSubscriptionRepository;
import org.dhis2.fhir.adapter.fhir.metadata.repository.SystemCodeRepository;
import org.dhis2.fhir.adapter.fhir.metadata.repository.SystemRepository;
import org.dhis2.fhir.adapter.fhir.model.FhirVersion;
import org.dhis2.fhir.adapter.fhir.security.SystemAuthenticationToken;
import org.springframework.data.domain.Example;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The service that provides the initial setup.
 *
 * @author volsch
 */
@Service
public class SetupService
{
    public static final String ORGANIZATION_CODE_CATEGORY = "ORGANIZATION_UNIT";

    private final CodeCategoryRepository codeCategoryRepository;

    private final CodeRepository codeRepository;
    
    private final SystemRepository systemRepository;

    private final SystemCodeRepository systemCodeRepository;

    private final RemoteSubscriptionRepository remoteSubscriptionRepository;

    public SetupService( @Nonnull CodeCategoryRepository codeCategoryRepository, @Nonnull CodeRepository codeRepository,
        @Nonnull SystemRepository systemRepository, @Nonnull SystemCodeRepository systemCodeRepository,
        @Nonnull RemoteSubscriptionRepository remoteSubscriptionRepository )
    {
        this.codeCategoryRepository = codeCategoryRepository;
        this.codeRepository = codeRepository;
        this.systemRepository = systemRepository;
        this.systemCodeRepository = systemCodeRepository;
        this.remoteSubscriptionRepository = remoteSubscriptionRepository;
    }

    public boolean hasCompletedSetup()
    {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication( new SystemAuthenticationToken() );
        try
        {
            return (remoteSubscriptionRepository.count() > 0);
        }
        finally
        {
            SecurityContextHolder.getContext().setAuthentication( authentication );
        }
    }

    @Transactional
    public void apply( @Nonnull Setup setup )
    {
        final System organizationSystem = findOrCreateSystem(
            setup.getRemoteSubscriptionSetup().getSystemUriSetup().getOrganizationSystemUri(),
            "Default DHIS2 Organization Units", "DEFAULT_DHIS2_ORG_UNIT", "Default DHIS2 organization unit system URI." );
        final System patientSystem = findOrCreateSystem(
            setup.getRemoteSubscriptionSetup().getSystemUriSetup().getPatientSystemUri(),
            "Default DHIS2 Patients", "DEFAULT_DHIS2_PATIENT", "Default DHIS2 patient system URI." );

        createRemoteSubscription( setup.getRemoteSubscriptionSetup(), organizationSystem, patientSystem );
        createSystemCodes( setup.getOrganizationCodeSetup(), organizationSystem );
    }

    private void createSystemCodes( @Nonnull OrganizationCodeSetup setup, @Nonnull System organizationSystem )
    {
        final List<FhirDhisCodeMapping> codeMappings = setup.getCodeMappings();
        if ( !codeMappings.isEmpty() )
        {
            final CodeCategory organizationCodeCategory = findCodeCategory( ORGANIZATION_CODE_CATEGORY );
            final Map<String, Code> persistedCodes = codeRepository.saveAll( createCodes( codeMappings, organizationCodeCategory ).values() )
                .stream().collect( Collectors.toMap( Code::getMappedCode, c -> c ) );

            final List<SystemCode> systemCodes = codeMappings.stream().map( cm -> {
                final SystemCode systemCode = new SystemCode();
                systemCode.setSystem( organizationSystem );
                systemCode.setSystemCode( cm.getFhirCode() );
                systemCode.setCode( persistedCodes.get( cm.getDhisCode() ) );
                return systemCode;
            } ).collect( Collectors.toList() );
            systemCodeRepository.saveAll( systemCodes );
        }
    }

    @Nonnull
    private Map<String, Code> createCodes( @Nonnull List<FhirDhisCodeMapping> codeMappings, @Nonnull CodeCategory organizationCodeCategory )
    {
        final Map<String, Code> codes = new HashMap<>();
        codeMappings.forEach( cm -> {
            codes.computeIfAbsent( cm.getDhisCode(), c -> {
                final Code code = new Code();
                code.setCodeCategory( organizationCodeCategory );
                code.setName( "Default DHIS2 Organization Unit Code " + c );
                code.setDescription( "Default DHIS2 organization unit code " + c );
                code.setCode( c.startsWith( OrganizationCodeSetup.DEFAULT_CODE_PREFIX ) ? c :
                    (OrganizationCodeSetup.DEFAULT_CODE_PREFIX + c) );
                code.setMappedCode( c );
                return code;
            } );
        } );
        return codes;
    }

    private void createRemoteSubscription( @Nonnull RemoteSubscriptionSetup setup, @Nonnull System organizationSystem, @Nonnull System patientSystem )
    {
        final RemoteSubscription remoteSubscription = new RemoteSubscription();
        remoteSubscription.setSystems( new ArrayList<>() );
        remoteSubscription.setAutoCreatedSubscriptionResources( Collections.singleton( FhirResourceType.PATIENT ) );
        remoteSubscription.setName( "Default Remote Subscription" );
        remoteSubscription.setCode( "DEFAULT" );
        remoteSubscription.setDescription( "Default remote subscription." );
        remoteSubscription.setFhirVersion( FhirVersion.DSTU3 );
        remoteSubscription.setEnabled( true );
        remoteSubscription.setToleranceMillis( setup.getFhirSetup().getToleranceMillis() );

        final SubscriptionAdapterEndpoint adapterEndpoint = new SubscriptionAdapterEndpoint();
        adapterEndpoint.setBaseUrl( setup.getAdapterSetup().getBaseUrl() );
        adapterEndpoint.setAuthorizationHeader( setup.getAdapterSetup().getAuthorizationHeaderValue() );
        adapterEndpoint.setSubscriptionType( setup.getFhirSetup().getSubscriptionType() );
        remoteSubscription.setAdapterEndpoint( adapterEndpoint );

        final SubscriptionDhisEndpoint dhisEndpoint = new SubscriptionDhisEndpoint();
        dhisEndpoint.setAuthenticationMethod( AuthenticationMethod.BASIC );
        dhisEndpoint.setUsername( setup.getDhisSetup().getUsername() );
        dhisEndpoint.setPassword( setup.getDhisSetup().getPassword() );
        remoteSubscription.setDhisEndpoint( dhisEndpoint );

        final SubscriptionFhirEndpoint fhirEndpoint = new SubscriptionFhirEndpoint();
        fhirEndpoint.setBaseUrl( setup.getFhirSetup().getBaseUrl() );
        if ( StringUtils.isNotBlank( setup.getFhirSetup().getHeaderName() ) &&
            StringUtils.isNotBlank( setup.getFhirSetup().getHeaderValue() ) )
        {
            fhirEndpoint.setHeaders( new TreeSet<>( Collections.singleton( new RequestHeader(
                setup.getFhirSetup().getHeaderName(),
                setup.getFhirSetup().getHeaderValue(), true ) ) ) );
        }
        remoteSubscription.setFhirEndpoint( fhirEndpoint );

        final RemoteSubscriptionSystem subscriptionOrganizationSystem = new RemoteSubscriptionSystem();
        subscriptionOrganizationSystem.setRemoteSubscription( remoteSubscription );
        subscriptionOrganizationSystem.setFhirResourceType( FhirResourceType.ORGANIZATION );
        subscriptionOrganizationSystem.setSystem( organizationSystem );
        subscriptionOrganizationSystem.setCodePrefix( setup.getSystemUriSetup().getOrganizationCodePrefix() );
        remoteSubscription.getSystems().add( subscriptionOrganizationSystem );

        final RemoteSubscriptionSystem subscriptionPatientSystem = new RemoteSubscriptionSystem();
        subscriptionPatientSystem.setRemoteSubscription( remoteSubscription );
        subscriptionPatientSystem.setFhirResourceType( FhirResourceType.PATIENT );
        subscriptionPatientSystem.setSystem( patientSystem );
        subscriptionPatientSystem.setCodePrefix( setup.getSystemUriSetup().getPatientCodePrefix() );
        remoteSubscription.getSystems().add( subscriptionPatientSystem );

        remoteSubscriptionRepository.saveAndFlush( remoteSubscription );
    }

    @Nonnull
    protected System findOrCreateSystem( @Nonnull String systemUri, @Nonnull String name, @Nonnull String code, @Nonnull String description )
    {
        System system = new System();
        system.setSystemUri( systemUri );
        system = systemRepository.findAll( Example.of( system ) ).stream().findFirst().orElse( null );
        if ( system == null )
        {
            system = new System();
            system.setName( name );
            system.setCode( code );
            system.setDescription( description );
            system.setSystemUri( systemUri );
            system.setEnabled( true );
            system = systemRepository.save( system );
        }
        return system;
    }

    @Nonnull
    protected CodeCategory findCodeCategory( @Nonnull String code )
    {
        CodeCategory codeCategory = new CodeCategory();
        codeCategory.setCode( code );
        codeCategory = codeCategoryRepository.findAll( Example.of( codeCategory ) ).stream().findFirst().orElse( null );
        if ( codeCategory == null )
        {
            throw new SetupException( "Code category with code " + code + " does not exist." );
        }
        return codeCategory;
    }
}