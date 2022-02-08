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
package com.github.nfalco79.bitbucket.client.rest;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.github.nfalco79.bitbucket.client.model.Permission;

@XmlRootElement
public class PrivilegesRequest {

    public static class Principal {
        private String accountId;
        private String slug;

        @XmlElement(name = "account_id")
        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }
    }

    private List<Principal> principals = new LinkedList<>();
    private Permission privilege;

    public List<Principal> getPrincipals() {
        return principals;
    }

    public void setPrincipals(List<Principal> principals) {
        this.principals = principals;
    }

    public Permission getPrivilege() {
        return privilege;
    }

    public void setPrivilege(Permission privilege) {
        this.privilege = privilege;
    }
}
