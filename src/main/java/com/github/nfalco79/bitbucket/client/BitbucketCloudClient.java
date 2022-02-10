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

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.TimeValue;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nfalco79.bitbucket.client.Credentials.OAuth2Consumer;
import com.github.nfalco79.bitbucket.client.internal.rest.BranchPermissionResponse;
import com.github.nfalco79.bitbucket.client.internal.rest.GroupPermissionResponse;
import com.github.nfalco79.bitbucket.client.internal.rest.PaginatedResponse;
import com.github.nfalco79.bitbucket.client.internal.rest.PullRequestActivityResponse;
import com.github.nfalco79.bitbucket.client.internal.rest.PullRequestResponse;
import com.github.nfalco79.bitbucket.client.internal.rest.RepositoryResponse;
import com.github.nfalco79.bitbucket.client.internal.rest.UserPermissionResponse;
import com.github.nfalco79.bitbucket.client.internal.rest.WebhookResponse;
import com.github.nfalco79.bitbucket.client.model.Activity;
import com.github.nfalco79.bitbucket.client.model.Approval;
import com.github.nfalco79.bitbucket.client.model.AuthToken;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction;
import com.github.nfalco79.bitbucket.client.model.GroupInfo;
import com.github.nfalco79.bitbucket.client.model.Permission;
import com.github.nfalco79.bitbucket.client.model.PullRequest;
import com.github.nfalco79.bitbucket.client.model.Repository;
import com.github.nfalco79.bitbucket.client.model.UserInfo;
import com.github.nfalco79.bitbucket.client.model.UserPermission;
import com.github.nfalco79.bitbucket.client.model.Webhook;

/**
 * Client of Bitbucket Cloud.
 * 
 * @author Nikolas Falco
 */
public class BitbucketCloudClient implements Closeable {
    private static final String HEADER_CSRF = "X-Atlassian-Token";

    @SuppressWarnings("unused")
    private static final String AUTHORIZATION_USER_PLACEHOLDER = "x-token-auth";

    private static final String PATH_PARAM_PR_ID = "pull_request_id";
    private static final String PATH_PARAM_REPOSITORY = "repository";
    private static final String PATH_PARAM_WORKSPACE = "workspace";
    private static final String PATH_PARAM_GROUP = "group";
    private static final String PATH_PARAM_GROUPOWNER = "group_owner";
    private static final String QUERY_PARAM_TERM = "term";
    private static final String QUERY_PARAM_HAS_ACCESS = "hasAccess";
    private static final String QUERY_PARAM_PAGELEN = "pagelen";
    private static final String QUERY_PARAM_QUERY = "q";
    private static final String QUERY_PARAM_FIELDS = "fields";

    private static final String FORM_PARAM_GRANT_TYPE = "grant_type";
    private static final String FORM_PARAM_REFRESH_TOKEN = "refresh_token";
    @SuppressWarnings("unused")
    private static final String GRANT_TYPE_AC = "authorization_code";
    private static final String GRANT_TYPE_CC = "client_credentials";
    @SuppressWarnings("unused")
    private static final String GRANT_TYPE_JWT = "urn:bitbucket:oauth2:jwt";
    private static final String GRANT_TYPE_REFRESH = "refresh_token";

    private static final String DEFAULT_PAGE_LEN = "100";

    // deprecated 1.0 API not available in 2.0
    private static final String API_V1 = "https://api.bitbucket.org/1.0";
    private static final String GROUP_API_V1 = API_V1 + "/group-privileges/{workspace}/{repository}/{group_owner}/{group}";

    // not documented APIs
    private static final String NO_API_V1 = "https://bitbucket.org/!api/1.0";
    private static final String GROUP_NO_API = NO_API_V1 + "/group-privileges/{workspace}/{repository}";

    // internal APIs
    private static final String INTERNAL_API = "https://bitbucket.org/!api/internal";
    private static final String GROUP_NO_API_INTERNAL = INTERNAL_API + "/_group-privileges-to-add/{workspace}/{repository}";
    @SuppressWarnings("unused")
    private static final String PRIVILEGES_NO_API_INTERNAL = INTERNAL_API + "/user-and-group-privileges/{workspace}/{repository}";
    private static final String USER_NO_API_INTERNAL = INTERNAL_API + "/repositories/{workspace}/{repository}/users";

    // REST 2.0 APIs
    private static final String OAUTH2 = "https://bitbucket.org/site/oauth2/access_token";
    private static final String API_V2 = "https://api.bitbucket.org/2.0";
    private static final String WORKSPACE = API_V2 + "/workspaces/{workspace}";
    private static final String PERMISSIONS = WORKSPACE + "/permissions/repositories/{repository}";

    private static final String REPOSITORY = API_V2 + "/repositories/{workspace}";
    private static final String REPOSITORY_BRANCH_RESTRICTIONS = REPOSITORY + "/{repository}/branch-restrictions";
    private static final String REPOSITORY_WEBHOOKS = REPOSITORY + "/{repository}/hooks";
    private static final String REPOSITORY_PR = REPOSITORY + "/{repository}/pullrequests";
    private static final String REPOSITORY_PR_ACTIVITY = REPOSITORY_PR + "/{pull_request_id}/activity";
    private static final String REPOSITORY_PR_APPROVE = REPOSITORY_PR + "/{pull_request_id}/approve";

    private static final String USER = API_V2 + "/user";
    private static final String USER_PERMISSIONS = USER + "/permissions/repositories";

    protected final Logger logger = Logger.getLogger("BitcketCloudClient");

    private Credentials credentials;
    private int retry = 3;
    private boolean dryRun;
    private AuthToken authToken;
    private LocalDateTime tokenExpiration;
    private transient UserInfo loggedUser;
    private CloseableHttpClient client;
    protected ObjectMapper objectMapper;

    /**
     * BBClient constructor which requires server info.
     *
     * @param credentials the object containing the server info
     */
    public BitbucketCloudClient(Credentials credentials) {
        this.credentials = credentials;
        buildJSONConverter();
        buildClient();
        buildAuthentication();
    }

    private void buildAuthentication() {
        if (hasOAuth()) {
            LocalDateTime now = LocalDateTime.now();
            try {
                authToken = getOAuthToken();
                tokenExpiration = now.plusSeconds(authToken.getExpiry());
                ((OAuth2Consumer) credentials).setToken(authToken.getAccessToken());
            } catch (ClientException e) {
                logger.log(Level.SEVERE, "Fail to acquire OAuth2 access token", e);
            }
        }
    }

    private <T> List<T> getPaginated(String uri, Class<? extends PaginatedResponse<T>> type) throws ClientException {
        List<T> result = new ArrayList<T>();
        while (uri != null) {
            PaginatedResponse<T> page = process(new HttpGet(uri), type);
            uri = page.getNext();
            result.addAll(page.getValues());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected <T> T process(HttpUriRequest request, Object type) throws ClientException {
        CloseableHttpResponse response = null;
        try {
            setupRequest(request);
            response = client.execute(request);
        } catch (IOException e) {
            throw new ClientException("Client fails on URL " + request.getRequestUri(), e);
        }

        if (response.getCode() >= HttpStatus.SC_OK //
                && response.getCode() < 300) {
            try {
                if (type instanceof Class) {
                    return objectMapper.readValue(response.getEntity().getContent(), (Class<T>) type);
                } else if (type instanceof TypeReference) {
                    return objectMapper.readValue(response.getEntity().getContent(), (TypeReference<T>) type);
                } else {
                    return null;
                }
            } catch (UnsupportedOperationException | IOException e) {
                throw new ClientException("Fail to deserialize response.", e);
            }
        } else if (response.getCode() == HttpStatus.SC_UNAUTHORIZED && isTokenExpired()) {
            authToken = refreshToken();
            tokenExpiration = LocalDateTime.now().plusSeconds(authToken.getExpiry());
        } else if (response.getCode() >= HttpStatus.SC_BAD_REQUEST //
                && response.getCode() < HttpStatus.SC_SERVER_ERROR //
                && response.getCode() != HttpStatus.SC_CONFLICT) { // conflict
            throw new ClientException(response);
        }

        throw new ClientException(response);
    }

    private <T> T process(HttpUriRequest request) throws ClientException {
        return process(request, null);
    }

    /**
     * Get logged user details
     *
     * @return user details
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public UserInfo getUser() throws ClientException {
        return process(new HttpGet(USER), UserInfo.class);
    }

    /**
     * Validate connection to bitbucket.org.
     *
     * @return {@code true} is connection and credentials are verified with
     *         success, {@code false} otherwise.
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public boolean testConnection() throws ClientException {
        try {
            loggedUser = getUser();
        } catch (ClientException e) {
            return false;
        }
        return true;
    }

    /**
     * Get all repositories of given workspace for which this user have read
     * access.
     *
     * @param workspace bitbucket workspace
     * @return a list of repositories.
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public List<Repository> getRepositories(String workspace) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(REPOSITORY) //
                .query(QUERY_PARAM_PAGELEN) //
                .build() //
                .set(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .expand();
        return getPaginated(requestURI, RepositoryResponse.class);
    }

    /**
     * Get the current user's access right on the given repository.
     *
     * @param repository name
     * @return the privilege of this user on the repository
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public Permission getPermission(String repository) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(USER_PERMISSIONS) //
                .query(QUERY_PARAM_PAGELEN, QUERY_PARAM_QUERY) //
                .build() //
                .set(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .set(QUERY_PARAM_QUERY, "repository.name=\"" + repository + "\"") //
                .expand();
        List<UserPermission> permissions = getPaginated(requestURI, UserPermissionResponse.class);
        Permission higher = Permission.NONE;
        for (UserPermission p : permissions) {
            if (p.getPermission().ordinal() > higher.ordinal()) {
                higher = p.getPermission();
            }
        }
        return higher;
    }

    /**
     * Find groups which doesn't have access rights to the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param workspace name
     * @param repository name
     * @return all the existing groups that doesn't have access rights to the
     *         repository
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public List<String> findAvailableGroups(String workspace, String repository) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(GROUP_NO_API_INTERNAL).build() //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand();
        List<GroupInfo> groups = process(new HttpGet(requestURI), new TypeReference<List<GroupInfo>>() {});
        return groups.stream().map(GroupInfo::getSlug).collect(Collectors.toList());
    }

    /**
     * Find groups which have access rights to the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param workspace name
     * @param repository the repository name
     * @return the groups that have access right to the repository, with their
     *         privilege
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public Map<String, Permission> getGroupsPermission(String workspace, String repository) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(GROUP_NO_API) //
                .build() //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand() + "?exclude-members=1";
        // Each group associated to its privilege
        List<GroupPermissionResponse> data = process(new HttpGet(requestURI), new TypeReference<List<GroupPermissionResponse>>() {});
        return data.stream() //
                .collect(Collectors.toMap(group -> group.getGroup().getSlug(), GroupPermissionResponse::getPrivilege));
    }

    /**
     * Find users which have single access rights to the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param workspace name
     * @param repository the repository name
     * @param filter username that start with
     * @return the groups that have access right to the repository, with their
     *         privilege
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public List<UserInfo> findUser(String workspace, String repository, String filter) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(USER_NO_API_INTERNAL) //
                .query(QUERY_PARAM_HAS_ACCESS, QUERY_PARAM_TERM) //
                .build() //
                .set(QUERY_PARAM_HAS_ACCESS, false) //
                .set(QUERY_PARAM_TERM, filter) //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand();
        return process(new HttpGet(requestURI), new TypeReference<List<UserInfo>>() {});
    }

    /**
     * Returns branch restriction for the given repository.
     *
     * @param workspace name
     * @param repository the repository name
     * @return the list of branch restriction setup for this repository
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public List<BranchRestriction> getBranchRestrictions(String workspace, String repository) throws ClientException {
        String uri = UriTemplate.buildFromTemplate(REPOSITORY_BRANCH_RESTRICTIONS) //
                .query(QUERY_PARAM_PAGELEN) //
                .build() //
                .set(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand();
        return getPaginated(uri, BranchPermissionResponse.class);
    }

    /**
     * Grant access right to a group or update the current group's privilege to
     * the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param workspace name
     * @param repository the repository name
     * @param groupName the group name
     * @param accessLevel read, write or admin access level
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public void updateGroupAccess(String workspace, String repository, String groupName, Permission accessLevel) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(GROUP_API_V1) //
                .build() //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .set(PATH_PARAM_GROUPOWNER, workspace) //
                .set(PATH_PARAM_GROUP, groupName) //
                .expand();

        HttpPut request = new HttpPut(requestURI);
        request.setEntity(new StringEntity(accessLevel.toString().toLowerCase()));
        request.addHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=utf-8");
        process(request);
    }

    /**
     * Get user right on a specified repository.
     *
     * @param workspace name
     * @param repository the repository name
     * @param nickname bitbucker nickname
     * @return user permission for given repository
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public UserPermission getUserPermission(String workspace, String repository, String nickname) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(PERMISSIONS) //
                .query(QUERY_PARAM_QUERY) //
                .build() //
                .set(QUERY_PARAM_QUERY, "user.nickname=\"" + nickname + "\"") //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand();
        UserPermissionResponse data = process(new HttpGet(requestURI), UserPermissionResponse.class);
        if (data.getValues().isEmpty()) {
            UserPermission none = new UserPermission();
            none.setPermission(Permission.NONE);
            return none;
        }
        return data.getValues().get(0);
    }

    /*
     * This INTERNAL API does not work anymore, it require a session
     * authentication
     */
    public void updateUserAccess(String repository, List<String> usernameIds, Permission accessLevel) {
        logger.log(Level.SEVERE, "Users {0} must be registered manually for {1} rights on repository {2}, no API works at the moment.", //
                new Object[] { usernameIds, accessLevel, repository });
        // URI requestURI =
        // UriBuilder.fromUri(PRIVILEGES_NO_API_INTERNAL).build(OWNER,
        // repository);
        //
        // PrivilegesRequest payload = new PrivilegesRequest();
        // usernameIds.forEach(id -> {
        // Principal p = new Principal();
        // p.setAccountId(id);
        // payload.getPrincipals().add(p);
        // });
        // payload.setPrivilege(accessLevel);
        // try (Response response = post(createBuilder(requestURI), payload)) {}
    }

    /**
     * Delete group that shouldn't have any access right to the given
     * repository.
     *
     * @param workspace name
     * @param repository the repository name
     * @param groupName the group name
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public void deleteGroupAccess(String workspace, String repository, String groupName) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(GROUP_API_V1) //
                .build() //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .set(PATH_PARAM_GROUPOWNER, workspace) //
                .set(PATH_PARAM_GROUP, groupName) //
                .expand();
        process(new HttpDelete(requestURI));
    }

    /**
     * Add or update the given branch permission for the specified repository.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param permission to add or update
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public void updateBranchRestriction(String workspace, String repository, BranchRestriction permission) throws ClientException {
        UriTemplateBuilder template = UriTemplate.buildFromTemplate(REPOSITORY_BRANCH_RESTRICTIONS);

        HttpUriRequestBase request;
        if (permission.getId() != null) {
            String requestURI = template.path("{id}") //
                    .build() //
                    .set(PATH_PARAM_WORKSPACE, workspace) //
                    .set(PATH_PARAM_REPOSITORY, repository) //
                    .set("id", permission.getId()) //
                    .expand();
            request = new HttpPut(requestURI);
        } else {
            String requestURI = template.build() //
                    .set(PATH_PARAM_WORKSPACE, workspace) //
                    .set(PATH_PARAM_REPOSITORY, repository) //
                    .expand();
            request = new HttpPost(requestURI);
        }
        request.setEntity(asJSONEntity(permission));
        process(request);
    }

    /**
     * Gets all repository web hooks.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param hookName a filter based on partial match of the web hook name
     * @return list of web hook
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public List<Webhook> getWebhooks(String workspace, String repository, String... hookName) throws ClientException {

        String uri = UriTemplate.buildFromTemplate(REPOSITORY_WEBHOOKS) //
                .query(QUERY_PARAM_PAGELEN) //
                .build() //
                .set(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand();
        List<Webhook> webhooks = getPaginated(uri, WebhookResponse.class);
        if (hookName != null && hookName.length > 0) {
            return webhooks.stream() //
                    .filter(hook -> Arrays.asList(hookName).contains(hook.getDescription())) //
                    .collect(Collectors.toList());
        }
        return webhooks;
    }

    /**
     * Updates web hook.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param webhook name
     * @return created web hook
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public Webhook updateWebhook(String workspace, String repository, Webhook webhook) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(REPOSITORY_WEBHOOKS) //
                .path("{id}") //
                .build() //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .set("id", webhook.getUUID()) //
                .expand();
        HttpPut request = new HttpPut(requestURI);
        request.setEntity(asJSONEntity(webhook));
        return process(request, Webhook.class);
    }

    /**
     * Deletes web hook.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param webhookId web hook identifier
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public void deleteWebhook(String workspace, String repository, String webhookId) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(REPOSITORY_WEBHOOKS).path("{id}") //
                .build() //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .set("id", webhookId) //
                .expand();
        process(new HttpDelete(requestURI));
    }

    /**
     * Creates a new web hook in the specified repository.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param webhook to create
     * @return created web hook
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public Webhook addWebHook(String workspace, String repository, Webhook webhook) throws ClientException {
        String requestURI = UriTemplate.fromTemplate(REPOSITORY_WEBHOOKS) //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand();
        HttpPost request = new HttpPost(requestURI);
        request.setEntity(asJSONEntity(webhook));
        return process(request, Webhook.class);
    }

    /**
     * Gets all pull request of specified repository.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @return list of pull requests
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public List<PullRequest> getPullRequests(String workspace, String repository) throws ClientException {
        String requestURI = UriTemplate.fromTemplate(REPOSITORY_PR) //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .expand();
        return getPaginated(requestURI, PullRequestResponse.class);
    }

    /**
     * Gets all pull request approval for the specified pull request.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param prId pull request identifier
     * @return list user approvals
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public List<Approval> getPullRequestApprovals(String workspace, String repository, int prId) throws ClientException {
        String requestURI = UriTemplate.buildFromTemplate(REPOSITORY_PR_ACTIVITY) //
                .query(QUERY_PARAM_FIELDS) //
                .build() //
                .set(QUERY_PARAM_FIELDS, "values.approval") //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .set(PATH_PARAM_PR_ID, prId) //
                .expand();
        List<Activity> activities = getPaginated(requestURI, PullRequestActivityResponse.class);
        return activities.stream() //
                .map(Activity::getApproval) //
                .filter(Objects::nonNull) //
                .collect(Collectors.toList());
    }

    /**
     * Returns if current user has approved the specified pull request.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param prId pull request identifier
     * @return {@code true} is this user has approved the pull request,
     *         {@code false} otherwise
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public boolean isPullRequestApproved(String workspace, String repository, int prId) throws ClientException {
        if (loggedUser == null) {
            loggedUser = getUser();
        }
        return getPullRequestApprovals(workspace, repository, prId).stream() //
                .anyMatch(approval -> loggedUser.getUUID().equals(approval.getUser().getUUID()));
    }

    /**
     * Approve or disapprove the specified pull request.
     *
     * @param workspace bitbucket
     * @param repository the repository name
     * @param prId pull request identifier
     * @param approval if approve or not
     * @throws ClientException in case of HTTP response from server different
     *         than 20x codes
     */
    public void setPullRequestApproval(String workspace, String repository, int prId, boolean approval) throws ClientException {
        String requestURI = UriTemplate.fromTemplate(REPOSITORY_PR_APPROVE) //
                .set(PATH_PARAM_WORKSPACE, workspace) //
                .set(PATH_PARAM_REPOSITORY, repository) //
                .set(PATH_PARAM_PR_ID, prId) //
                .expand();

        if (approval) {
            process(new HttpPost(requestURI));
        } else {
            process(new HttpDelete(requestURI));
        }
    }

    private void setupRequest(HttpUriRequest request) throws ClientException {
        addHeader(request, HttpHeaders.ACCEPT, "application/json;charset=utf-8");
        addHeader(request, HEADER_CSRF, "no-check");
        credentials.apply(request);
    }

    private void addHeader(HttpUriRequest request, String key, String value) {
        if (request.getFirstHeader(key) == null) {
            request.addHeader(key, value);
        }
    }

    private AuthToken refreshToken() throws ClientException {
        HttpPost request = new HttpPost(OAUTH2);
        request.setEntity(new UrlEncodedFormEntity(Arrays.asList( //
                new BasicNameValuePair(FORM_PARAM_GRANT_TYPE, GRANT_TYPE_REFRESH), //
                new BasicNameValuePair(FORM_PARAM_REFRESH_TOKEN, authToken.getRefreshToken()) //
        )));

        return process(request, AuthToken.class);
    }

    private AuthToken getOAuthToken() throws ClientException {
        HttpPost request = new HttpPost(OAUTH2);
        request.setEntity(new UrlEncodedFormEntity(Arrays.asList( //
                new BasicNameValuePair(FORM_PARAM_GRANT_TYPE, GRANT_TYPE_CC))));
        return process(request, AuthToken.class);
    }

    private boolean hasOAuth() {
        return credentials instanceof OAuth2Consumer;
    }

    private boolean isTokenExpired() {
        return hasOAuth() && authToken != null && LocalDateTime.now().isAfter(tokenExpiration);
    }

    protected void buildClient() {
        // BasicCredentialsProvider credsProvider = new
        // BasicCredentialsProvider();
        client = HttpClients.custom() //
                // .setDefaultCredentialsProvider(credsProvider) //
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(retry, TimeValue.ofSeconds(2))) //
                .build();
    }

    private void buildJSONConverter() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private HttpEntity asJSONEntity(Object object) throws ClientException {
        try {
            return new StringEntity(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            throw new ClientException("Fail to serialize permission", e);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
        buildClient();
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

}
