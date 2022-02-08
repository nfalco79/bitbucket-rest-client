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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnumValue;

public class Repository extends BitbucketObject {
    public enum ForkPolicy {
        ALLOW, //
        @XmlEnumValue("no_forks")
        NOT_ALLOW
    }

    private String slug;
    private Project project;
    private Workspace workspace;
    private UserInfo owner;
    private boolean isPrivate;
    private ForkPolicy forkPolicy;

    public Repository() {
    }

    public Repository(String name) {
        this.slug = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public UserInfo getOwner() {
        return owner;
    }

    public void setOwner(UserInfo owner) {
        this.owner = owner;
    }

    @XmlElement(name = "is_private")
    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public ForkPolicy getForkPolicy() {
        return forkPolicy;
    }

    public void setForkPolicy(ForkPolicy forkPolicy) {
        this.forkPolicy = forkPolicy;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }
}