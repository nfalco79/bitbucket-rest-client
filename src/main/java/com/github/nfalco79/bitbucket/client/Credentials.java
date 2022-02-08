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

public abstract class Credentials {

    public static class CredentialsBuilder {
        public static Credentials oauth2(String clientId, String secret) {
            return new OAuth2Consumer(clientId, secret);
        }

        public static Credentials appPassword(String user, String password) {
            return new AppPassword(user, password);
        }
    }

    public static class AppPassword extends Credentials {
        private AppPassword(String user, String password) {
            super(user, password);
        }
    }

    public static class OAuth2Consumer extends Credentials {
        private OAuth2Consumer(String user, String password) {
            super(user, password);
        }
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
}
