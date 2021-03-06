package org.dhis2.fhir.adapter.fhir.metadata.model;

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

import org.dhis2.fhir.adapter.model.VersionedBaseMetadata;
import org.dhis2.fhir.adapter.validator.EnumValue;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Contains the subscription related system URI assignments.
 *
 * @author volsch
 */
@Entity
@Table( name = "fhir_server_system" )
public class FhirServerSystem extends VersionedBaseMetadata implements Serializable
{
    private static final long serialVersionUID = -930459310559544662L;

    public static final int MAX_CODE_PREFIX_LENGTH = 20;

    @NotNull
    private FhirServer fhirServer;

    @NotNull
    @EnumValue( FhirResourceType.class )
    private FhirResourceType fhirResourceType;

    @NotNull
    private System system;

    @Size( max = MAX_CODE_PREFIX_LENGTH )
    private String codePrefix;

    @Basic
    @Column( name = "fhir_resource_type", nullable = false, length = 30 )
    @Enumerated( EnumType.STRING )
    public FhirResourceType getFhirResourceType()
    {
        return fhirResourceType;
    }

    public void setFhirResourceType( FhirResourceType fhirResource )
    {
        this.fhirResourceType = fhirResource;
    }

    @ManyToOne
    @JoinColumn( name = "fhir_server_id", nullable = false )
    public FhirServer getFhirServer()
    {
        return fhirServer;
    }

    public void setFhirServer( FhirServer fhirServer )
    {
        this.fhirServer = fhirServer;
    }

    @ManyToOne
    @JoinColumn( name = "system_id", nullable = false )
    public System getSystem()
    {
        return system;
    }

    public void setSystem( System system )
    {
        this.system = system;
    }

    @Basic
    @Column( name = "code_prefix", length = 20 )
    public String getCodePrefix()
    {
        return codePrefix;
    }

    public void setCodePrefix( String codePrefix )
    {
        this.codePrefix = codePrefix;
    }
}
