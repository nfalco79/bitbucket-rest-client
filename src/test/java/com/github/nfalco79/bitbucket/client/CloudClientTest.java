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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.nfalco79.bitbucket.client.model.Approval;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction.Builder;
import com.github.nfalco79.bitbucket.client.model.Permission;
import com.github.nfalco79.bitbucket.client.model.PullRequest;
import com.github.nfalco79.bitbucket.client.model.Repository;
import com.github.nfalco79.bitbucket.client.model.UserInfo;

public class CloudClientTest {

    private static final String WORKSPACE = "nfalco79";

    private BitbucketCloudClient client;
    private Collection<URI> uriCalls;

    @Before
    public void setupClient() throws Exception {
        // LogManager.getLogManager().readConfiguration(getClass().getResourceAsStream("/logging.properties"));

        uriCalls = new LinkedList<>();
        client = new BitbucketCloudClient(Mockito.mock(Credentials.class)) {
            @SuppressWarnings("unchecked")
            @Override
            protected <T> T process(HttpUriRequest request, Object type) throws ClientException {
                try {
                    URI requestURI = request.getUri();
                    uriCalls.add(requestURI);
                    String resource = "/" + requestURI.getHost() + requestURI.getPath() + "/response.json";
                    try (InputStream is = CloudClientTest.class.getResourceAsStream(resource)) {
                        if (type instanceof Class) {
                            return objectMapper.readValue(is, (Class<T>) type);
                        } else if (type instanceof TypeReference) {
                            return objectMapper.readValue(is, (TypeReference<T>) type);
                        } else {
                            return null;
                        }
                    } catch (UnsupportedOperationException | IOException e) {
                        throw new ClientException("Fail to deserialize response.", e);
                    }
                } catch (URISyntaxException e) {
                    throw new RuntimeException("", e);
                }
            }
        };
    }

    @Test
    public void get_logged_user() throws Exception {
        UserInfo userInfo = client.getUser();
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getNickname()).isNotNull();
    }

    @Test
    public void get_repositories() throws Exception {
        List<Repository> repositories = client.getRepositories(WORKSPACE);
        assertThat(repositories).isNotEmpty();
    }

    @Test
    public void branch_restrictions() throws Exception {
        List<BranchRestriction> restrictions = client.getBranchRestrictions(WORKSPACE, "test-repos");
        assertThat(restrictions).contains(Builder.newDeletePermission("master")) //
                .contains(Builder.newForcePushPermission("master")) //
                .contains(Builder.newMinApprovalsPermission("master", 2));
    }

    @Test
    public void user_permission() throws Exception {
        Permission rights = client.getPermission("test-repos");
        assertThat(rights).isEqualTo(Permission.ADMIN);
    }

    @Test
    public void test_group_permission() throws Exception {
        Map<String, Permission> rights = client.getGroupsPermission("finantix", "tds.plt.platform-java");
        assertThat(rights).containsEntry("administrators", Permission.ADMIN) //
                .containsEntry("prd_plt_ftx_ld", Permission.WRITE);
    }

    @Test
    public void find_user() throws Exception {
        List<UserInfo> users = client.findUser("finantix", "tds.plt.platform-java", "radbui");
        assertThat(users).anySatisfy(user -> assertThat(user.getNickname()).isEqualTo("radbuilder"));
    }

    @Test
    public void find_missing_groups() throws Exception {
        List<String> groups = client.findAvailableGroups("finantix", "tds.plt.platform-java");
        assertThat(groups).isNotEmpty();
    }

    @Test
    public void get_pullrequests() throws Exception {
        List<PullRequest> pullRequests = client.getPullRequests(WORKSPACE, "test-repos");
        assertThat(pullRequests).isNotEmpty().anySatisfy(pr -> {
            assertThat(pr.getDestination().getCommit().getHash()).isEqualTo("bf4f4ce8a3a8");
            assertThat(pr.getTitle()).isEqualTo("Add one message more");
            assertThat(pr.getDescription()).isEqualTo("test from forked repo");
        });
    }

    @Test
    public void get_pullrequest_approval() throws Exception {
        List<Approval> approvals = client.getPullRequestApprovals("nfalco79", "test-repos", 1);
        assertThat(approvals).isNotEmpty().anySatisfy(approval -> {
            assertThat(approval.getUser().getNickname()).isEqualTo("Nikolas Falco");
            assertThat(approval.getPullRequest().getId()).isEqualTo(1);
        });
    }

    @Test
    public void approve_pullrequest() throws Exception {
        boolean approved = client.isPullRequestApproved("nfalco79", "test-repos", 1);
        client.setPullRequestApproval("nfalco79", "test-repos", 1, !approved);
        client.setPullRequestApproval("nfalco79", "test-repos", 1, approved);

        assertThat(uriCalls).contains(new URI("https://api.bitbucket.org/2.0/repositories/nfalco79/test-repos/pullrequests/1/approve"));
    }
}