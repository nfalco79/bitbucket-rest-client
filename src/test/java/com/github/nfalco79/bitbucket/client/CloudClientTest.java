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

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.nfalco79.bitbucket.client.model.BranchRestriction;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction.Builder;
import com.github.nfalco79.bitbucket.client.model.Permission;
import com.github.nfalco79.bitbucket.client.model.Repository;
import com.github.nfalco79.bitbucket.client.model.UserInfo;

public class CloudClientTest {

    private static final String WORKSPACE = "nfalco79";

    protected BitbucketCloudClient client;

    @Before
    public void setupClient() {
        client = new BitbucketCloudClientTest(Mockito.mock(Credentials.class));
    }

    @Test
    public void test_get_logged_user() throws Exception {
        UserInfo userInfo = client.getUser();
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getNickname()).isNotNull();
    }

    @Test
    public void test_get_repositories() throws Exception {
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
    public void test_user_permission() throws Exception {
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
        List<String> groups = client.findAvailableGroups(WORKSPACE, "test-repos");
        assertThat(groups).isNotEmpty();
    }
}