package org.dhis2.fhir.adapter;

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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.dhis2.fhir.adapter.dhis.model.ReferenceType;
import org.dhis2.fhir.adapter.dhis.security.SecurityConfig;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirResourceType;
import org.dhis2.fhir.adapter.fhir.metadata.model.SubscriptionType;
import org.dhis2.fhir.adapter.fhir.security.AdapterSystemAuthenticationToken;
import org.dhis2.fhir.adapter.lock.LockManager;
import org.dhis2.fhir.adapter.lock.impl.EmbeddedLockManagerImpl;
import org.dhis2.fhir.adapter.script.ScriptCompiler;
import org.dhis2.fhir.adapter.script.impl.ScriptCompilerImpl;
import org.dhis2.fhir.adapter.setup.Setup;
import org.dhis2.fhir.adapter.setup.SetupResult;
import org.dhis2.fhir.adapter.setup.SetupService;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Configuration that must be executed before any test is run. It setups the
 * database with a basic system setup.
 *
 * @author volsch
 */
@Configuration
@AutoConfigureAfter( HibernateJpaAutoConfiguration.class )
public class TestConfiguration
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String ADAPTER_AUTHORIZATION = "Bearer 836ef9274abc828728746";

    public static final String DHIS2_USERNAME = "fhir_user";

    public static final String DHIS2_PASSWORD = "fhir_user_123";

    public static final String FHIR_SERVICE_HEADER_NAME = "Authorization";

    public static final String FHIR_SERVICE_HEADER_VALUE = "Basic Zmhpcjp0ZXN0";

    public static final String BASE_DSTU3_CONTEXT = "/baseDstu3";

    @Value( "${dhis2.fhir-adapter.endpoint.system-authentication.username}" )
    private String dhis2SystemAuthenticationUsername;

    @Value( "${dhis2.fhir-adapter.endpoint.system-authentication.password}" )
    private String dhis2SystemAuthenticationPassword;

    @Autowired
    private SetupService setupService;

    private WireMockServer fhirMockServer;

    private UUID fhirServerId;

    private Map<FhirResourceType, UUID> fhirServerResourceIds;

    @Nonnull
    public UUID getFhirServerId()
    {
        return fhirServerId;
    }

    @Nonnull
    public UUID getFhirServerResourceId( @Nonnull FhirResourceType resourceType )
    {
        final UUID resourceId = fhirServerResourceIds.get( resourceType );
        Assert.assertNotNull( "FHIR server resource for " + resourceType + " could not be found.", resourceId );
        return resourceId;
    }

    @Nonnull
    public String getDhis2SystemAuthorization()
    {
        return "Basic " + Base64.getEncoder().encodeToString(
            (dhis2SystemAuthenticationUsername + ":" + dhis2SystemAuthenticationPassword).getBytes( StandardCharsets.UTF_8 ) );
    }

    @Nonnull
    public String getDhis2UserAuthorization()
    {
        return "Basic " + Base64.getEncoder().encodeToString(
            (DHIS2_USERNAME + ":" + DHIS2_PASSWORD).getBytes( StandardCharsets.UTF_8 ) );
    }

    @Nonnull
    @Bean
    @Primary
    protected AbstractUserDetailsAuthenticationProvider testDhisWebApiAuthenticationProvider( @Nonnull SecurityConfig securityConfig )
    {
        return new TestDhisWebApiAuthenticationProvider( securityConfig );
    }

    @Nonnull
    @Bean
    @Primary
    protected LockManager embeddedLockManager()
    {
        return new EmbeddedLockManagerImpl();
    }

    @Nonnull
    @Bean
    protected ScriptCompiler scriptCompiler( @Value( "${dhis2.fhir-adapter.transformation.script-engine-name}" ) @Nonnull String scriptEngineName )
    {
        return new ScriptCompilerImpl( scriptEngineName );
    }

    @Nonnull
    @Bean
    protected WireMockServer fhirMockServer()
    {
        return fhirMockServer;
    }

    @PostConstruct
    protected void postConstruct()
    {
        fhirMockServer = new WireMockServer( wireMockConfig().dynamicPort() );
        fhirMockServer.start();
        logger.info( "Started WireMock server for FHIR requests on port {}.", fhirMockServer.port() );

        final Setup setup = new Setup();

        setup.getFhirServerSetup().getAdapterSetup().setBaseUrl( "http://localhost:8081" );
        setup.getFhirServerSetup().getAdapterSetup().setAuthorizationHeaderValue( ADAPTER_AUTHORIZATION );

        setup.getFhirServerSetup().getDhisSetup().setUsername( DHIS2_USERNAME );
        setup.getFhirServerSetup().getDhisSetup().setPassword( DHIS2_PASSWORD );

        setup.getFhirServerSetup().getFhirSetup().setBaseUrl( fhirMockServer.baseUrl() + BASE_DSTU3_CONTEXT );
        setup.getFhirServerSetup().getFhirSetup().setHeaderName( FHIR_SERVICE_HEADER_NAME );
        setup.getFhirServerSetup().getFhirSetup().setHeaderValue( FHIR_SERVICE_HEADER_VALUE );
        setup.getFhirServerSetup().getFhirSetup().setSubscriptionType( SubscriptionType.REST_HOOK );
        setup.getFhirServerSetup().getFhirSetup().setSupportsRelatedPerson( true );

        setup.getFhirServerSetup().getSystemUriSetup().setOrganizationSystemUri( "http://example.sl/organizations" );
        setup.getFhirServerSetup().getSystemUriSetup().setOrganizationCodePrefix( "OU_" );
        setup.getFhirServerSetup().getSystemUriSetup().setPatientSystemUri( "http://example.sl/patients" );
        setup.getFhirServerSetup().getSystemUriSetup().setPatientCodePrefix( "PT_" );

        setup.getOrganizationCodeSetup().setFallback( true );
        setup.getOrganizationCodeSetup().setDefaultDhisCode( "OU_4567" );
        setup.getOrganizationCodeSetup().setMappings( "9876 OU_1234 \n  8765, OU_2345" );

        setup.getTrackedEntitySetup().getFirstName().setReferenceType( ReferenceType.CODE );
        setup.getTrackedEntitySetup().getFirstName().setReferenceValue( "FIRST_NAME" );
        setup.getTrackedEntitySetup().getLastName().setReferenceType( ReferenceType.NAME );
        setup.getTrackedEntitySetup().getLastName().setReferenceValue( "Last Name" );

        final SetupResult setupResult;
        SecurityContextHolder.getContext().setAuthentication( new AdapterSystemAuthenticationToken() );
        try
        {
            Assert.assertFalse( setupService.hasCompletedSetup() );
            setupResult = setupService.apply( setup );
            Assert.assertTrue( setupService.hasCompletedSetup() );
        }
        finally
        {
            SecurityContextHolder.clearContext();
        }

        this.fhirServerId = setupResult.getFhirServerId();
        this.fhirServerResourceIds = setupResult.getFhirServerResourceIds();
    }

    @PreDestroy
    protected void preDestroy()
    {
        if ( fhirMockServer != null )
        {
            fhirMockServer.stop();
        }
    }
}
