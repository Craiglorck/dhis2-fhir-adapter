package org.dhis2.fhir.adapter.jackson;

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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Configures Jackson for the application.
 *
 * @author volsch
 */
@Configuration
@Validated
public class JacksonConfig
{
    @Bean
    @Order( 1 )
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer()
    {
        return jacksonObjectMapperBuilder -> {
            final ZoneId zoneId = ZoneId.systemDefault();
            jacksonObjectMapperBuilder
                .serializers(
                    new ZonedDateTimeSerializer(),
                    new LocalDateSerializer( DateTimeFormatter.ISO_LOCAL_DATE ) )
                .deserializers(
                    new ZonedDateTimeDeserializer(),
                    new LocalDateDeserializer( DateTimeFormatter.ISO_LOCAL_DATE ) );
            jacksonObjectMapperBuilder.filters( new SimpleFilterProvider()
                .addFilter( SecuredPropertyFilter.FILTER_NAME, new SecuredPropertyFilter() )
                .addFilter( JsonCachePropertyFilter.FILTER_NAME, new SimpleBeanPropertyFilter()
                {
                } ) );
            jacksonObjectMapperBuilder.featuresToDisable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        };
    }
}
