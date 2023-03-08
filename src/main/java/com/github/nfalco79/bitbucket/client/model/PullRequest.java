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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PullRequest implements Serializable {
    private static final long serialVersionUID = -7636025949730656228L;

    private Integer id;
    private String type;
    private String title;
    private String description;
    private Date date;
    private boolean closeSourceBranch;
    private Links links;
    private String state;
    private int taskCount;
    private int commentsCount;
    private String reason;
    private UserInfo author;
    private List<UserInfo> reviewers = new ArrayList<>();
    private UserInfo closedBy;
    private BitbucketReference source;
    private BitbucketReference destination;

    @JsonProperty("close_source_branch")
    public boolean isCloseSourceBranch() {
        return closeSourceBranch;
    }

    public void setCloseSourceBranch(boolean closeSourceBranch) {
        this.closeSourceBranch = closeSourceBranch;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UserInfo getAuthor() {
        return author;
    }

    public void setAuthor(UserInfo author) {
        this.author = author;
    }

    public UserInfo getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UserInfo closedBy) {
        this.closedBy = closedBy;
    }

    public BitbucketReference getSource() {
        return source;
    }

    public void setSource(BitbucketReference source) {
        this.source = source;
    }

    public BitbucketReference getDestination() {
        return destination;
    }

    public void setDestination(BitbucketReference destination) {
        this.destination = destination;
    }

    public List<UserInfo> getReviewers() {
        return reviewers;
    }

    public void setReviewers(List<UserInfo> reviewers) {
        this.reviewers = reviewers;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
