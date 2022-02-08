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

import java.lang.reflect.Method;
import java.net.URI;

import org.assertj.core.api.Assertions;
import org.glassfish.jersey.client.ClientRequest;

public class ClientUtil {

    public static URI getRequetsURI(javax.ws.rs.client.Invocation.Builder builder) {
        org.glassfish.jersey.client.JerseyInvocation.Builder b = (org.glassfish.jersey.client.JerseyInvocation.Builder) builder;
        try {
            Method m = b.getClass().getDeclaredMethod("request");
            m.setAccessible(true);
            ClientRequest request = (ClientRequest) m.invoke(b);
            return request.getUri();
        } catch (ReflectiveOperationException | SecurityException e) {
            Assertions.fail("Fail to get request", e);
        }
        return null;
    }
}