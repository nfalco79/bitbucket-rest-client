/*
 * Copyright 2022 Falco Nikolas
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.bitbucket.client;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class BitbucketCloudClientTest extends BitbucketCloudClient {
    private class ResponseAnswer implements Answer<Object> {
        private URI requestURI;

        public ResponseAnswer(URI uri) {
            this.requestURI = uri;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Object[] arguments = invocation.getArguments();
            Class targetType;
            Type genericType;
            if (arguments[0] instanceof GenericType) {
                targetType = ((GenericType) arguments[0]).getRawType();
                genericType = ((GenericType) arguments[0]).getType();
            } else {
                targetType = (Class) arguments[0];
                genericType = targetType;
            }
            MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;
            if (arguments.length == 2) {
                mediaType = invocation.getArgument(1);
            }
            String resource = "/" + requestURI.getHost() + requestURI.getPath() + "/response.json";
            try (InputStream is = CloudClientTest.class.getResourceAsStream(resource)) {
                return provider.readFrom(targetType, genericType, new Annotation[0], mediaType, null, is);
            }
        }
    };

    public JacksonJaxbJsonProvider provider;

    public BitbucketCloudClientTest(Credentials credentials) {
        super(credentials);
    }

    @Override
    protected JacksonJaxbJsonProvider jaxbProvider() {
        provider = super.jaxbProvider();
        return provider;
    }

    @SuppressWarnings("unchecked")
    private Response processRequest(javax.ws.rs.client.Invocation.Builder builder) {
        URI uri = ClientUtil.getRequetsURI(builder);
        Response spy = Mockito.spy(Response.ok().build());
        Mockito.doAnswer(new ResponseAnswer(uri)).when(spy).readEntity(Mockito.any(Class.class));
        Mockito.doAnswer(new ResponseAnswer(uri)).when(spy).readEntity(Mockito.any(GenericType.class));
        return spy;
    }

    @Override
    protected Response get(javax.ws.rs.client.Invocation.Builder builder) throws ClientException {
        return processRequest(builder);
    }

    @Override
    protected <T> Response post(Builder request, T payload) throws ClientException {
        return processRequest(request);
    }

    @Override
    protected <T> Response put(Builder request, T payload) throws ClientException {
        return processRequest(request);
    }

    @Override
    protected <T> Response put(Builder request, T payload, MediaType mediaType) throws ClientException {
        return processRequest(request);
    }
}