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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Webhook {
    public static final String REPO_PUSH = "repo:push";
    public static final String PULLREQUEST_CREATED = "pullrequest:created";
    public static final String PULLREQUEST_UPDATED = "pullrequest:updated";
    public static final String PULLREQUEST_REJECTED = "pullrequest:rejected";
    public static final String PULLREQUEST_FULFILLED = "pullrequest:fulfilled";

    private String uuid;
    private String description;
    private String url;
    private boolean active = true;
    private List<String> events = new LinkedList<String>();

    @XmlElement(name = "uuid")
    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, events, url);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Webhook)) {
            return false;
        }
        Webhook other = (Webhook) obj;
        return active == other.active //
                && (!Objects.isNull(events) && !Objects.isNull(other.events) && events.containsAll(other.events)) //
                && Objects.equals(url, other.url);
    }

    @Override
    public String toString() {
        return description + " " + url;
    }

}