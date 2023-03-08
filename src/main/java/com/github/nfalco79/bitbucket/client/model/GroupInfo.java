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
package com.github.nfalco79.bitbucket.client.model;

import java.util.Objects;

/**
 * Include the basic details relative to the group to identify it.
 */

public class GroupInfo extends BitbucketObject {
    private static final long serialVersionUID = 6204004493199315972L;

    private String slug;
    private UserInfo owner;

    public GroupInfo() {
        setType("group");
    }

    public GroupInfo(String slug) {
        this();
        this.slug = slug;
        setName(slug);
    }

    /**
     * Get the slug that identifies a BB group.
     *
     * @return the slug, else null
     */
    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public UserInfo getOwner() {
        return owner;
    }

    public void setOwner(UserInfo owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GroupInfo other = (GroupInfo) obj;
        return Objects.equals(getType(), other.getType()) && Objects.equals(getName(), other.getName());
    }

    @Override
    public String toString() {
        return getName() == null ? slug : getName();
    }
}