package org.dhis2.fhir.adapter.fhir.metadata.model;

/*
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.dhis2.fhir.adapter.dhis.model.DhisResourceType;
import org.dhis2.fhir.adapter.fhir.transform.model.FhirResourceType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A rule defines a business rules and transformations from a FHIR resource to a DHIS2 resource and vice versa.
 *
 * @author volsch
 */
@Entity
@Table( name = "fhir_rule" )
@Inheritance( strategy = InheritanceType.JOINED )
@DiscriminatorColumn( name = "dhis_resource_type", discriminatorType = DiscriminatorType.STRING )
@Cache( region = "rule", usage = CacheConcurrencyStrategy.READ_WRITE )
public abstract class Rule implements Serializable
{
    private static final long serialVersionUID = 3426378271314934021L;

    private UUID id;
    private Long version;
    private LocalDateTime createdAt;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
    private String name;
    private String description;
    private boolean enabled;
    private int evaluationOrder;
    private FhirResourceType fhirResourceType;
    private DhisResourceType dhisResourceType;
    private ExecutableScript applicableInScript;
    private ExecutableScript transformInScript;

    @GeneratedValue( generator = "uuid2" )
    @GenericGenerator( name = "uuid2", strategy = "uuid2" )
    @Id @Column( name = "id", nullable = false ) public UUID getId()
    {
        return id;
    }

    public void setId( UUID id )
    {
        this.id = id;
    }

    @Basic @Column( name = "version", nullable = false ) public Long getVersion()
    {
        return version;
    }

    public void setVersion( Long version )
    {
        this.version = version;
    }

    @Basic @Column( name = "created_at", nullable = false ) public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt( LocalDateTime createdAt )
    {
        this.createdAt = createdAt;
    }

    @Basic @Column( name = "last_updated_by", length = 11 ) public String getLastUpdatedBy()
    {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy( String lastUpdatedBy )
    {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    @Basic @Column( name = "last_updated_at", nullable = false ) public LocalDateTime getLastUpdatedAt()
    {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt( LocalDateTime lastUpdatedAt )
    {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    @Basic @Column( name = "name", nullable = false, length = 230 ) public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Basic @Column( name = "description", length = -1 ) public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Basic @Column( name = "enabled", nullable = false ) public boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    @Basic @Column( name = "evaluation_order", nullable = false ) public int getEvaluationOrder()
    {
        return evaluationOrder;
    }

    public void setEvaluationOrder( int evaluationOrder )
    {
        this.evaluationOrder = evaluationOrder;
    }

    @Basic @Column( name = "fhir_resource_type", nullable = false, length = 30 ) @Enumerated( EnumType.STRING ) public FhirResourceType getFhirResourceType()
    {
        return fhirResourceType;
    }

    public void setFhirResourceType( FhirResourceType fhirResourceType )
    {
        this.fhirResourceType = fhirResourceType;
    }

    @Basic @Column( name = "dhis_resource_type", nullable = false, length = 30 ) @Enumerated( EnumType.STRING ) public DhisResourceType getDhisResourceType()
    {
        return dhisResourceType;
    }

    public void setDhisResourceType( DhisResourceType dhisResourceType )
    {
        this.dhisResourceType = dhisResourceType;
    }

    @ManyToOne @JoinColumn( name = "applicable_in_script_id", referencedColumnName = "id" ) public ExecutableScript getApplicableInScript()
    {
        return applicableInScript;
    }

    public void setApplicableInScript( ExecutableScript applicableInScript )
    {
        this.applicableInScript = applicableInScript;
    }

    @ManyToOne @JoinColumn( name = "transform_in_script_id", referencedColumnName = "id", nullable = false ) public ExecutableScript getTransformInScript()
    {
        return transformInScript;
    }

    public void setTransformInScript( ExecutableScript transformInScript )
    {
        this.transformInScript = transformInScript;
    }
}
