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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;

/**
 * This object represent the credentials to use in {@link BitbucketCloudClient}.
 * <p>
 * The implementations also setup a request with proper authentication.
 *
 * @author Nikolas Falco
 */
public abstract class Credentials {

    public static class CredentialsBuilder {
        public static Credentials oauth2(String clientId, String secret) {
            return new OAuth2Consumer(clientId, secret);
        }

        public static Credentials appPassword(String user, String password) {
            return new AppPassword(user, password);
        }

        public static Credentials anonymous() {
            return new Credentials(null, null) {
                @Override
                public void apply(HttpRequest request) {
                }
            };
        }

        public static Credentials build(String key, String secret) {
            if (key == null || secret == null) {
                throw new IllegalArgumentException("key or secreat is missing");
            }

            if (isOAuthCredentials(key, secret)) {
                return oauth2(key, secret);
            } else {
                return appPassword(key, secret);
            }
        }

        private static boolean isOAuthCredentials(String key, String secret) {
            return !key.contains("@") && key.length() == 18 && secret.length() == 32;
        }

    }

    /* package */ static class AppPassword extends Credentials {
        private AppPassword(String user, String password) {
            super(user, password);
        }

        @Override
        public void apply(HttpRequest request) {
            request.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuth(this));
        }
    }

    /* package */ static class OAuth2Consumer extends Credentials {
        private String accessToken;

        private OAuth2Consumer(String user, String password) {
            super(user, password);
        }

        @Override
        public void apply(HttpRequest request) {
            if ("/site/oauth2/access_token".equals(request.getRequestUri())) {
                request.setHeader(HttpHeaders.AUTHORIZATION, getBasicAuth(this));
            } else {
                request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }
        }

        public void setToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    private static String getBasicAuth(Credentials credentials) {
        return "Basic " + Base64.getEncoder().encodeToString((credentials.getUser() + ":"
                + credentials.getPassword()).getBytes(StandardCharsets.UTF_8));
    }

    private String user;
    private String password;

    private Credentials(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public abstract void apply(HttpRequest request);
}