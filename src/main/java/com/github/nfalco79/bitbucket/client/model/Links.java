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

import java.util.ArrayList;
import java.util.List;

public class Links {
    public static class Href {
        private String href;

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
    }

    private Href repositories;
    private Href watchers;
    private Href branches;
    private Href tags;
    private Href commits;
    private List<Href> clone = new ArrayList<>(2);
    private Href hooks;
    private Href self;
    private Href source;
    private Href html;
    private Href avatar;
    private Href snippets;
    private Href forks;
    private Href downloads;
    private Href pullrequests;
    private Href merge;
    private Href decline;
    private Href diff;
    private Href statuses;
    private Href approve;
    private Href activity;

    public Href getHooks() {
        return hooks;
    }

    public void setHooks(Href hooks) {
        this.hooks = hooks;
    }

    public Href getSelf() {
        return self;
    }

    public void setSelf(Href self) {
        this.self = self;
    }

    public Href getRepositories() {
        return repositories;
    }

    public void setRepositories(Href repositories) {
        this.repositories = repositories;
    }

    public Href getHtml() {
        return html;
    }

    public void setHtml(Href html) {
        this.html = html;
    }

    public Href getAvatar() {
        return avatar;
    }

    public void setAvatar(Href avatar) {
        this.avatar = avatar;
    }

    public Href getSnippets() {
        return snippets;
    }

    public void setSnippets(Href snippets) {
        this.snippets = snippets;
    }

    public Href getWatchers() {
        return watchers;
    }

    public void setWatchers(Href watchers) {
        this.watchers = watchers;
    }

    public Href getBranches() {
        return branches;
    }

    public void setBranches(Href branches) {
        this.branches = branches;
    }

    public Href getTags() {
        return tags;
    }

    public void setTags(Href tags) {
        this.tags = tags;
    }

    public Href getCommits() {
        return commits;
    }

    public void setCommits(Href commits) {
        this.commits = commits;
    }

    public List<Href> getClone() {
        return clone;
    }

    public void setClone(List<Href> clone) {
        this.clone = clone;
    }

    public Href getSource() {
        return source;
    }

    public void setSource(Href source) {
        this.source = source;
    }

    public Href getForks() {
        return forks;
    }

    public void setForks(Href forks) {
        this.forks = forks;
    }

    public Href getDownloads() {
        return downloads;
    }

    public void setDownloads(Href downloads) {
        this.downloads = downloads;
    }

    public Href getPullrequests() {
        return pullrequests;
    }

    public void setPullrequests(Href pullrequests) {
        this.pullrequests = pullrequests;
    }

    public Href getMerge() {
        return merge;
    }

    public void setMerge(Href merge) {
        this.merge = merge;
    }

    public Href getDecline() {
        return decline;
    }

    public void setDecline(Href decline) {
        this.decline = decline;
    }

    public Href getDiff() {
        return diff;
    }

    public void setDiff(Href diff) {
        this.diff = diff;
    }

    public Href getApprove() {
        return approve;
    }

    public void setApprove(Href approve) {
        this.approve = approve;
    }

    public Href getStatuses() {
        return statuses;
    }

    public void setStatuses(Href statuses) {
        this.statuses = statuses;
    }

    public Href getActivity() {
        return activity;
    }

    public void setActivity(Href activity) {
        this.activity = activity;
    }

}