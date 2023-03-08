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

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;


public class BranchRestriction implements Serializable {
    public static class Builder {
        public static BranchRestriction newPushPermission(String pattern, Set<UserInfo> users, Set<GroupInfo> groups) {
            BranchRestriction p = new BranchRestriction("push", pattern);
            p.setUsers(users);
            p.setGroups(groups);
            return p;
        }

        public static BranchRestriction newMergePermission(String pattern, Set<UserInfo> users, Set<GroupInfo> groups) {
            BranchRestriction p = new BranchRestriction("restrict_merges", pattern);
            p.setUsers(users);
            p.setGroups(groups);
            return p;
        }

        public static BranchRestriction newDeletePermission(String pattern) {
            return new BranchRestriction("delete", pattern);
        }

        public static BranchRestriction newForcePushPermission(String pattern) {
            return new BranchRestriction("force", pattern);
        }

        public static BranchRestriction newRequireNoChanges(String pattern) {
            return new BranchRestriction("require_no_changes_requested", pattern);
        }

        public static BranchRestriction newResetPROnChange(String pattern) {
            return new BranchRestriction("reset_pullrequest_changes_requested_on_change", pattern);
        }

        public static BranchRestriction newRequireTasksCompletion(String pattern) {
            return new BranchRestriction("require_tasks_to_be_completed", pattern);
        }

        public static BranchRestriction newMinApprovalsPermission(String pattern, int approvals) {
            BranchRestriction p = new BranchRestriction("require_approvals_to_merge", pattern);
            p.value = approvals;
            return p;
        }

        public static BranchRestriction newSucessBuildsPermission(String pattern, int count) {
            BranchRestriction p = new BranchRestriction("require_passing_builds_to_merge", pattern);
            p.value = count;
            return p;
        }

        public static void merge(BranchRestriction from, BranchRestriction to) {
            if (!from.getKind().equals(to.getKind())) {
                throw new IllegalArgumentException("Can not merge branch permission of kind " + from.getKind() + " with " + to.getKind());
            }
            switch (from.kind) {
            case "require_no_changes_requested":
            case "reset_pullrequest_changes_requested_on_change":
            case "require_tasks_to_be_completed":
            case "delete":
            case "force":
            case "require_approvals_to_merge":
            case "require_passing_builds_to_merge":
                to.setValue(from.getValue());
                break;
            case "push":
            case "restrict_merges":
                to.getUsers().addAll(from.getUsers());
                to.getGroups().addAll(from.getGroups());
                break;
            default:
                break;
            }
        }
    }

    private static final long serialVersionUID = -5772380756877266311L;

    private Integer id;
    private String kind;
    private String pattern;
    private String branchKind = "glob";
    private Integer value;
    private Links links;
    private Set<UserInfo> users = new LinkedHashSet<>();
    private Set<GroupInfo> groups = new LinkedHashSet<>();

    public BranchRestriction() {
    }

    public BranchRestriction(String kind, String pattern) {
        this.kind = kind;
        this.pattern = pattern;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @JsonProperty("branch_match_kind")
    public String getBranchKind() {
        return branchKind;
    }

    public void setBranchKind(String branchKind) {
        this.branchKind = branchKind;
    }

    public Set<UserInfo> getUsers() {
        return users;
    }

    public void setUsers(Set<UserInfo> users) {
        this.users = users;
    }

    public Set<GroupInfo> getGroups() {
        return groups;
    }

    public void setGroups(Set<GroupInfo> groups) {
        this.groups = groups;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groups, kind, pattern, users, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BranchRestriction)) {
            return false;
        }
        BranchRestriction other = (BranchRestriction) obj;
        return Objects.equals(groups, other.groups) //
                && Objects.equals(kind, other.kind) //
                && Objects.equals(pattern, other.pattern) //
                && Objects.equals(users, other.users) //
                && Objects.equals(value, other.value);
    }


    @Override
    public String toString() {
        return kind + " " + pattern;
    }

}