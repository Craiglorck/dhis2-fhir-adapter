package org.dhis2.fhir.adapter.fhir.data.model;

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

import org.dhis2.fhir.adapter.data.model.QueuedItemId;
import org.dhis2.fhir.adapter.fhir.metadata.model.FhirServerResource;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

/**
 * The unique ID of a distinct FHIR resource that has been queued for processing.
 *
 * @author volsch
 */
@Embeddable
public class QueuedFhirResourceId extends QueuedItemId<FhirServerResource> implements Serializable
{
    private static final long serialVersionUID = -4642534319215405587L;

    private FhirServerResource group;

    private String fhirResourceId;

    public QueuedFhirResourceId()
    {
        super();
    }

    public QueuedFhirResourceId( @Nonnull FhirServerResource group, @Nonnull String fhirResourceId )
    {
        this.group = group;
        this.fhirResourceId = fhirResourceId;
    }

    @ManyToOne( optional = false, fetch = FetchType.LAZY )
    @JoinColumn( name = "fhir_server_resource_id" )
    @Override
    public FhirServerResource getGroup()
    {
        return group;
    }

    @Override
    public void setGroup( FhirServerResource group )
    {
        this.group = group;
    }

    @Column( name = "fhir_resource_id", nullable = false )
    public String getFhirResourceId()
    {
        return fhirResourceId;
    }

    public void setFhirResourceId( String versionedFhirResourceId )
    {
        this.fhirResourceId = versionedFhirResourceId;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;
        QueuedFhirResourceId that = (QueuedFhirResourceId) o;
        return Objects.equals( (group == null) ? null : group.getId(),
            (that.group == null) ? null : that.group.getId() ) &&
            Objects.equals( fhirResourceId, that.fhirResourceId );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( (group == null) ? null : group.getId(), fhirResourceId );
    }

    @Override
    public String toString()
    {
        return "[Remote Subscription Resource ID " + ((group == null) ? "?" : group.getId()) + ", FHIR Resource ID " + getFhirResourceId() + "]";
    }
}
