package org.dhis2.fhir.adapter.fhir.transform.fhir.impl;

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
import org.dhis2.fhir.adapter.dhis.model.DhisResource;
import org.dhis2.fhir.adapter.dhis.model.Reference;
import org.dhis2.fhir.adapter.dhis.orgunit.OrganizationUnit;
import org.dhis2.fhir.adapter.dhis.orgunit.OrganizationUnitService;
import org.dhis2.fhir.adapter.dhis.tracker.trackedentity.TrackedEntityAttribute;
import org.dhis2.fhir.adapter.dhis.tracker.trackedentity.TrackedEntityAttributes;
import org.dhis2.fhir.adapter.dhis.tracker.trackedentity.TrackedEntityInstance;
import org.dhis2.fhir.adapter.dhis.tracker.trackedentity.TrackedEntityService;
import org.dhis2.fhir.adapter.dhis.tracker.trackedentity.TrackedEntityType;
import org.dhis2.fhir.adapter.fhir.data.repository.FhirDhisAssignmentRepository;
import org.dhis2.fhir.adapter.fhir.metadata.model.AbstractRule;
import org.dhis2.fhir.adapter.fhir.metadata.model.ExecutableScript;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirResourceMapping;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirResourceType;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirServerResource;
import org.dhis2.fhir.adapter.fhir.metadata.model.RuleInfo;
import org.dhis2.fhir.adapter.fhir.metadata.model.ScriptVariable;
import org.dhis2.fhir.adapter.fhir.metadata.model.TrackedEntityRule;
import org.dhis2.fhir.adapter.fhir.model.FhirVersion;
import org.dhis2.fhir.adapter.fhir.script.ScriptExecutionException;
import org.dhis2.fhir.adapter.fhir.script.ScriptExecutor;
import org.dhis2.fhir.adapter.fhir.transform.FatalTransformerException;
import org.dhis2.fhir.adapter.fhir.transform.TransformerContext;
import org.dhis2.fhir.adapter.fhir.transform.TransformerException;
import org.dhis2.fhir.adapter.fhir.transform.TransformerMappingException;
import org.dhis2.fhir.adapter.fhir.transform.TransformerRequestException;
import org.dhis2.fhir.adapter.fhir.transform.fhir.FhirToDhisTransformOutcome;
import org.dhis2.fhir.adapter.fhir.transform.fhir.FhirToDhisTransformerContext;
import org.dhis2.fhir.adapter.fhir.transform.fhir.TrackedEntityInstanceNotFoundException;
import org.dhis2.fhir.adapter.fhir.transform.fhir.impl.util.AbstractIdentifierFhirToDhisTransformerUtils;
import org.dhis2.fhir.adapter.fhir.transform.fhir.model.ResourceSystem;
import org.dhis2.fhir.adapter.fhir.transform.util.TransformerUtils;
import org.dhis2.fhir.adapter.geo.Location;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract base class for all transformers from FHIR to DHIS 2 resources.
 *
 * @param <R> the concrete type of the FHIR resource.
 * @param <U> the concrete type of transformation rule that this transformer processes.
 * @author volsch
 */
public abstract class AbstractFhirToDhisTransformer<R extends DhisResource, U extends AbstractRule> implements FhirToDhisTransformer<R, U>
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ScriptExecutor scriptExecutor;

    private final OrganizationUnitService organizationUnitService;

    private final TrackedEntityService trackedEntityService;

    private final FhirDhisAssignmentRepository fhirDhisAssignmentRepository;

    public AbstractFhirToDhisTransformer( @Nonnull ScriptExecutor scriptExecutor, @Nonnull OrganizationUnitService organizationUnitService, @Nonnull ObjectProvider<TrackedEntityService> trackedEntityService,
        @Nonnull FhirDhisAssignmentRepository fhirDhisAssignmentRepository )
    {
        this.scriptExecutor = scriptExecutor;
        this.organizationUnitService = organizationUnitService;
        this.trackedEntityService = trackedEntityService.getIfAvailable();
        this.fhirDhisAssignmentRepository = fhirDhisAssignmentRepository;
    }

    @Nullable
    @Override
    public FhirToDhisTransformOutcome<R> transformCasted( @Nonnull FhirServerResource fhirServerResource, @Nonnull FhirToDhisTransformerContext context, @Nonnull IBaseResource input,
        @Nonnull RuleInfo<? extends AbstractRule> ruleInfo, @Nonnull Map<String, Object> scriptVariables ) throws TransformerException
    {
        return transform( fhirServerResource, context, input, new RuleInfo<>( getRuleClass().cast( ruleInfo.getRule() ), ruleInfo.getDhisDataReferences() ), scriptVariables );
    }

    @Nonnull
    protected TrackedEntityService getTrackedEntityService() throws FatalTransformerException
    {
        if ( trackedEntityService == null )
        {
            throw new FatalTransformerException( "Tracked entity service has not been provided." );
        }
        return trackedEntityService;
    }

    @Nonnull
    @Override
    public Set<FhirVersion> getFhirVersions()
    {
        return FhirVersion.ALL;
    }

    @Nonnull
    protected Optional<R> getResource( @Nonnull FhirServerResource fhirServerResource, @Nonnull FhirToDhisTransformerContext context,
        @Nonnull RuleInfo<U> ruleInfo, @Nonnull Map<String, Object> scriptVariables ) throws TransformerException
    {
        final String id = getDhisId( context, ruleInfo );
        R resource = getResourceById( id ).orElse( null );
        if ( resource != null )
        {
            return Optional.of( resource );
        }

        resource = getResourceByAssignment( fhirServerResource, context, ruleInfo, scriptVariables ).orElse( null );
        if ( resource != null )
        {
            return Optional.of( resource );
        }

        resource = getActiveResource( context, ruleInfo, scriptVariables, false ).orElse( null );
        if ( resource != null )
        {
            return Optional.of( resource );
        }

        resource = createResource( context, ruleInfo, id, scriptVariables, false );
        if ( (resource != null) && isSyncRequired( context, ruleInfo, scriptVariables ) )
        {
            // the active resource may have been created in the meantime
            resource = getActiveResource( context, ruleInfo, scriptVariables, true ).orElse( null );
            if ( resource != null )
            {
                return Optional.of( resource );
            }
            resource = createResource( context, ruleInfo, id, scriptVariables, true );
        }

        return Optional.ofNullable( resource );
    }

    protected abstract boolean isSyncRequired( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo, @Nonnull Map<String, Object> scriptVariables ) throws TransformerException;

    @Nonnull
    protected abstract Optional<R> getResourceById( @Nullable String id ) throws TransformerException;

    @Nonnull
    protected abstract Optional<R> getActiveResource(
        @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo,
        @Nonnull Map<String, Object> scriptVariables, boolean sync ) throws TransformerException;

    @Nonnull
    protected Optional<R> getResourceByAssignment( @Nonnull FhirServerResource fhirServerResource,
        @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo, @Nonnull Map<String, Object> scriptVariables ) throws TransformerException
    {
        final IBaseResource fhirResource = TransformerUtils.getScriptVariable( scriptVariables, ScriptVariable.INPUT, IBaseResource.class );
        final String dhisResourceId = fhirDhisAssignmentRepository.findFirstDhisResourceId( ruleInfo.getRule(), fhirServerResource.getFhirServer(),
            fhirResource.getIdElement() );
        if ( dhisResourceId == null )
        {
            return Optional.empty();
        }
        return findResourceById( dhisResourceId );
    }

    @Nonnull
    protected abstract Optional<R> findResourceById( @Nonnull String id );

    @Nullable
    protected abstract R createResource( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo,
        @Nullable String id, @Nonnull Map<String, Object> scriptVariables, boolean sync ) throws TransformerException;

    @Nullable
    protected String getDhisId( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo )
    {
        final String resourceId = context.getFhirRequest().getResourceId();
        if ( (resourceId != null) && context.getFhirRequest().isFhirServer() )
        {
            throw new TransformerRequestException( "Requests that contain a resource ID " +
                "while processing a FHIR server are not supported." );
        }
        return resourceId;
    }

    protected boolean transform( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo, @Nonnull Map<String, Object> scriptVariables ) throws TransformerException
    {
        return Boolean.TRUE.equals( executeScript( context, ruleInfo, ruleInfo.getRule().getTransformImpScript(), scriptVariables, Boolean.class ) );
    }

    @Nonnull
    protected Optional<OrganizationUnit> getOrgUnit( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo, @Nonnull ExecutableScript lookupScript, @Nonnull Map<String, Object> scriptVariables )
    {
        final Reference orgUnitReference = executeScript( context, ruleInfo, lookupScript, scriptVariables, Reference.class );
        if ( orgUnitReference == null )
        {
            logger.info( "Could not extract organization unit reference." );
            return Optional.empty();
        }
        return getOrgUnit( context, orgUnitReference, scriptVariables );
    }

    @Nonnull
    protected Optional<OrganizationUnit> getOrgUnit( @Nonnull FhirToDhisTransformerContext context, @Nonnull Reference orgUnitReference, @Nonnull Map<String, Object> scriptVariables )
    {
        final Optional<OrganizationUnit> organisationUnit = organizationUnitService.findOneByReference( orgUnitReference );
        if ( !organisationUnit.isPresent() )
        {
            logger.info( "Organization unit of reference does not exist: " + orgUnitReference );
        }
        return organisationUnit;
    }

    @Nonnull
    protected Optional<TrackedEntityInstance> getTrackedEntityInstance( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<TrackedEntityRule> ruleInfo,
        @Nonnull FhirResourceMapping resourceMapping, @Nonnull Map<String, Object> scriptVariables, boolean sync )
    {
        final IBaseResource baseResource = getTrackedEntityInstanceResource( context, ruleInfo, resourceMapping, scriptVariables );
        if ( baseResource == null )
        {
            logger.info( "Tracked entity instance resource could not be extracted from input." );
            return Optional.empty();
        }
        final TrackedEntityInstance trackedEntityInstance =
            getTrackedEntityInstanceByIdentifier( context, ruleInfo, baseResource, scriptVariables, sync )
                .orElseThrow( () -> new TrackedEntityInstanceNotFoundException( "Tracked entity instance for FHIR Resource ID " + baseResource.getIdElement() + " could not be found.", baseResource ) );
        return Optional.of( trackedEntityInstance );
    }

    @Nullable
    protected IBaseResource getTrackedEntityInstanceResource( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<TrackedEntityRule> ruleInfo, @Nonnull FhirResourceMapping resourceMapping,
        @Nonnull Map<String, Object> scriptVariables ) throws TransformerException
    {
        return executeScript( context, ruleInfo, resourceMapping.getImpTeiLookupScript(), scriptVariables, IBaseResource.class );
    }

    @Nonnull
    protected Optional<TrackedEntityInstance> getTrackedEntityInstanceByIdentifier( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<TrackedEntityRule> ruleInfo, @Nonnull IBaseResource baseResource,
        @Nonnull Map<String, Object> scriptVariables, boolean sync ) throws TransformerException
    {
        String identifier = getIdentifier( context, baseResource, scriptVariables );
        if ( identifier == null )
        {
            return Optional.empty();
        }
        identifier = createFullQualifiedTrackedEntityInstanceIdentifier( context, baseResource, identifier );

        final TrackedEntityAttributes trackedEntityAttributes = TransformerUtils.getScriptVariable( scriptVariables, ScriptVariable.TRACKED_ENTITY_ATTRIBUTES, TrackedEntityAttributes.class );
        final TrackedEntityAttribute identifierAttribute = trackedEntityAttributes.getOptional( ruleInfo.getRule().getTrackedEntity().getTrackedEntityIdentifierReference() )
            .orElseThrow( () -> new TransformerMappingException( "Tracked entity identifier attribute does not exist: " + ruleInfo.getRule().getTrackedEntity().getTrackedEntityIdentifierReference() ) );

        final TrackedEntityType trackedEntityType = TransformerUtils.getScriptVariable( scriptVariables, ScriptVariable.TRACKED_ENTITY_TYPE, TrackedEntityType.class );
        final Collection<TrackedEntityInstance> result;
        if ( sync )
        {
            result = getTrackedEntityService().findByAttrValueRefreshed(
                trackedEntityType.getId(), identifierAttribute.getId(), Objects.requireNonNull( identifier ), 2 );
        }
        else
        {
            result = getTrackedEntityService().findByAttrValue(
                trackedEntityType.getId(), identifierAttribute.getId(), Objects.requireNonNull( identifier ), 2 );
        }
        if ( result.size() > 1 )
        {
            throw new TransformerMappingException( "Filtering with identifier of rule " + ruleInfo +
                " returned more than one tracked entity instance: " + identifier );
        }

        final String finalIdentifier = identifier;
        return result.stream().peek( tei -> tei.setIdentifier( finalIdentifier ) ).findFirst();
    }

    protected String createFullQualifiedTrackedEntityInstanceIdentifier( @Nonnull FhirToDhisTransformerContext context, IBaseResource baseResource, String identifier )
    {
        final FhirResourceType fhirResourceType = FhirResourceType.getByResource( baseResource );
        if ( fhirResourceType == null )
        {
            return identifier;
        }
        final ResourceSystem resourceSystem = context.getFhirRequest().getResourceSystem( fhirResourceType );
        if ( (resourceSystem != null) && StringUtils.isNotBlank( resourceSystem.getCodePrefix() ) )
        {
            identifier = resourceSystem.getCodePrefix() + identifier;
        }
        return identifier;
    }

    @Nullable
    protected String getIdentifier( @Nonnull FhirToDhisTransformerContext context, IBaseResource baseResource, @Nonnull Map<String, Object> scriptVariables )
    {
        if ( !(baseResource instanceof IDomainResource) )
        {
            throw new TransformerMappingException( "Resource " + baseResource.getClass().getSimpleName() +
                " is no domain resource and does not include the required identifier." );
        }

        final FhirResourceType resourceType = FhirResourceType.getByResource( baseResource );
        if ( resourceType == null )
        {
            throw new TransformerMappingException( "Could not map  " + baseResource.getClass().getSimpleName() + " to a FHIR resource." );
        }
        final ResourceSystem resourceSystem = context.getFhirRequest().getOptionalResourceSystem( resourceType )
            .orElseThrow( () -> new TransformerMappingException( "No system has been defined for resource type " + resourceType + "." ) );

        final String identifier = TransformerUtils.getScriptVariable( scriptVariables, ScriptVariable.IDENTIFIER_UTILS, AbstractIdentifierFhirToDhisTransformerUtils.class )
            .getResourceIdentifier( (IDomainResource) baseResource, resourceType, resourceSystem.getSystem() );
        if ( identifier == null )
        {
            logger.info( "Resource " + resourceType + " does not include the required identifier with system: " + resourceSystem.getSystem() );
            return null;
        }

        return identifier;
    }

    @Nullable
    protected Location getCoordinate( @Nonnull FhirToDhisTransformerContext context, @Nonnull RuleInfo<U> ruleInfo, @Nonnull ExecutableScript locationLookupScript, @Nonnull Map<String, Object> scriptVariable )
    {
        return executeScript( context, ruleInfo, locationLookupScript, scriptVariable, Location.class );
    }

    /**
     * Executes an executable script with the specified variables . If the mandatory data for executing
     * the script has not been provided, the script will not be executed at all.
     *
     * @param context          the transformer context of the transformation.
     * @param ruleInfo         the rule of the transformation.
     * @param executableScript the script that should be executed.
     * @param variables        the variables that the script requires.
     * @param resultClass      the type of the result the script returns.
     * @param <T>              the concrete class of the return type.
     * @return the result of the script or <code>null</code> if specified executable script is <code>null</code>.
     * @throws ScriptExecutionException thrown if the
     */
    @Nullable
    protected <T> T executeScript( @Nonnull TransformerContext context, @Nonnull RuleInfo<? extends AbstractRule> ruleInfo, @Nullable ExecutableScript executableScript, @Nonnull Map<String, Object> variables, @Nonnull Class<T> resultClass )
    {
        return TransformerUtils.executeScript( scriptExecutor, context, ruleInfo, executableScript, variables, resultClass );
    }
}
