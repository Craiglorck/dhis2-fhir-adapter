package org.dhis2.fhir.adapter.dhis.model;

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contains the different types of DHIS2 Resources that are can be created.
 *
 * @author volsch
 */
public enum DhisResourceType
{
    /**
     * Resource is a tracked entity instance.
     */
    TRACKED_ENTITY( "trackedEntityInstances" ),

    /**
     * Resource is a program instance (aka enrollment).
     */
    ENROLLMENT( "enrollments" ),

    /**
     * Resource is a program stage instance (aka event of a program instance).
     */
    PROGRAM_STAGE_EVENT( "events" ),

    /**
     * Resource is a organisation unit.
     */
    ORGANIZATION_UNIT( "organisationUnits" );

    private static final Map<String, DhisResourceType> byTypeName = Arrays.stream( values() ).collect( Collectors.toMap( DhisResourceType::getTypeName, v -> v ) );

    @Nullable
    public static DhisResourceType getByTypeName( @Nullable String typeName )
    {
        return byTypeName.get( typeName );
    }

    private final String typeName;

    DhisResourceType( @Nonnull String typeName )
    {
        this.typeName = typeName;
    }

    @Nonnull
    public String getTypeName()
    {
        return typeName;
    }
}

