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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.nfalco79.bitbucket.client.Credentials.OAuth2Consumer;
import com.github.nfalco79.bitbucket.client.model.AuthToken;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction;
import com.github.nfalco79.bitbucket.client.model.GroupInfo;
import com.github.nfalco79.bitbucket.client.model.Permission;
import com.github.nfalco79.bitbucket.client.model.Repository;
import com.github.nfalco79.bitbucket.client.model.UserInfo;
import com.github.nfalco79.bitbucket.client.model.UserPermission;
import com.github.nfalco79.bitbucket.client.model.Webhook;
import com.github.nfalco79.bitbucket.client.rest.BranchPermissionResponse;
import com.github.nfalco79.bitbucket.client.rest.GroupPermissionResponse;
import com.github.nfalco79.bitbucket.client.rest.PaginatedResponse;
import com.github.nfalco79.bitbucket.client.rest.RepositoryResponse;
import com.github.nfalco79.bitbucket.client.rest.UserPermissionResponse;
import com.github.nfalco79.bitbucket.client.rest.WebhookResponse;

/**
 * Client of Bitbucket Cloud.
 */
public class BitbucketCloudClient implements Closeable {
    private static final String HEADER_CSRF = "X-Atlassian-Token";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    @SuppressWarnings("unused")
    private static final String AUTHORIZATION_USER_PLACEHOLDER = "x-token-auth";

    private static final String QUERY_PARAM_TERM = "term";
    private static final String QUERY_PARAM_HAS_ACCESS = "hasAccess";
    private static final String QUERY_PARAM_EXCLUDE_MEMBERS = "exclude-members";
    private static final String QUERY_PARAM_PAGELEN = "pagelen";
    private static final String QUERY_PARAM_QUERY = "q";

    private static final String FORM_PARAM_GRANT_TYPE = "grant_type";
    private static final String FORM_PARAM_REFRESH_TOKEN = "refresh_token";
    @SuppressWarnings("unused")
    private static final String GRANT_TYPE_AC = "authorization_code";
    private static final String GRANT_TYPE_CC = "client_credentials";
    @SuppressWarnings("unused")
    private static final String GRANT_TYPE_JWT = "urn:bitbucket:oauth2:jwt";
    private static final String GRANT_TYPE_REFRESH = "refresh_token";

    private static final int DEFAULT_PAGE_LEN = 100;

    // deprecated 1.0 API not available in 2.0
    private static final String API_V1 = "https://api.bitbucket.org/1.0";
    private static final String GROUP_API_V1 = API_V1 + "/group-privileges/{workspace_id}/{repository}/{group_owner}/{group}";

    // not documented APIs
    private static final String NO_API_V1 = "https://bitbucket.org/!api/1.0";
    private static final String GROUP_NO_API = NO_API_V1 + "/group-privileges/{workspace_id}/{repository}";

    // internal APIs
    private static final String INTERNAL_API = "https://bitbucket.org/!api/internal";
    private static final String GROUP_NO_API_INTERNAL = INTERNAL_API + "/_group-privileges-to-add/{workspace_id}/{repository}";
    @SuppressWarnings("unused")
    private static final String PRIVILEGES_NO_API_INTERNAL = INTERNAL_API + "/user-and-group-privileges/{workspace_id}/{repository}";
    private static final String USER_NO_API_INTERNAL = INTERNAL_API + "/repositories/{workspace_id}/{repository}/users";

    // REST 2.0 APIs
    private static final String OAUTH2 = "https://bitbucket.org/site/oauth2/access_token";
    private static final String API_V2 = "https://api.bitbucket.org/2.0";
    private static final String WORKSPACE = API_V2 + "/workspaces/{workspace}";
    private static final String PERMISSIONS = WORKSPACE + "/permissions/repositories/{repository}";

    private static final String REPOSITORY = API_V2 + "/repositories/{workspace}";
    private static final String REPOSITORY_BRANCH_RESTRICTIONS = REPOSITORY + "/{repository}/branch-restrictions";
    private static final String REPOSITORY_WEBHOOKS = REPOSITORY + "/{repository}/hooks";
    private static final String REPOSITORY_WEBHOOKS_ID = REPOSITORY + "/{repository}/hooks/{uid}";

    private static final String USER = API_V2 + "/user";
    private static final String USER_PERMISSIONS = USER + "/permissions/repositories";

    protected final Logger logger = Logger.getLogger("BitcketCloudClient");

    private Credentials credentials;
    private Client client;
    private int retry;
    private boolean dryRun;

    private AuthToken authToken;
    private LocalDateTime tokenExpiration;

    /**
     * BBClient constructor which requires server info.
     *
     * @param credentials
     *            the object containing the server info
     */
    public BitbucketCloudClient(Credentials credentials) {
        this.credentials = credentials;
        // build a HTTP client
        buildClient();
    }

    private <T> List<T> getPaginated(URI uri, Class<? extends PaginatedResponse<T>> type) throws ClientException {
        List<T> result = new ArrayList<T>();
        while (uri != null) {
            try (Response response = get(createBuilder(uri))) {
                PaginatedResponse<T> data = response.readEntity(type);
                uri = data.getNext();
                result.addAll(data.getValues());
            }
        }
        return result;
    }

    protected Response get(Builder request) throws ClientException {
        return call(request::get);
    }

    protected Response delete(Builder request) throws ClientException {
        if (dryRun) {
            return Response.ok().build();
        }
        return call(request::delete);
    }

    protected <T> Response post(Builder request, T payload) throws ClientException {
        if (dryRun) {
            return Response.ok().build();
        }
        return call(() -> request.post(Entity.json(payload)));
    }

    protected <T> Response put(Builder request, T payload, MediaType mediaType) throws ClientException {
        if (dryRun) {
            return Response.ok().build();
        }
        return call(() -> request.put(Entity.entity(payload, mediaType)));
    }

    protected <T> Response put(Builder request, T payload) throws ClientException {
        if (dryRun) {
            return Response.ok().build();
        }
        return call(() -> request.put(Entity.json(payload)));
    }

    private Response call(Supplier<Response> callable) throws ClientException { // NOSONAR
        Response response = null;
        int retries = retry;
        do {
            response = callable.get();

            if (response.getStatus() >= 200 //
                    && response.getStatus() < 300) {
                return response;
            } else if (response.getStatus() == 401 && isTokenExpired()) {
                refreshToken();
            } else if (response.getStatus() >= 400 //
                    && response.getStatus() < 500 //
                    && response.getStatus() != Status.CONFLICT.getStatusCode()) {
                throw new ClientException("HTTP " + response.getStatus() + ": " + response.readEntity(String.class));
            }
            // otherwise let's retry
            doWait();
        } while (--retries > 0);

        throw new ClientException("Unexpected HTTP response " + response.getStatus() + ": " + response.readEntity(String.class));
    }

    protected void doWait() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            // start again retry
        }
    }

    /**
     * Get logged user details
     *
     * @return user details
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public UserInfo getUser() throws ClientException {
        URI uri = UriBuilder.fromUri(USER).build();
        try (Response response = get(createBuilder(uri))) {
            return response.readEntity(UserInfo.class);
        }
    }

    /**
     * Get all repositories of given workspace for which this user have read
     * access.
     *
     * @param workspace
     *            bitbucket workspace
     * @return a list of repositories.
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public List<Repository> getRepositories(String workspace) throws ClientException {
        URI uri = UriBuilder.fromUri(REPOSITORY) //
                .queryParam(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .build(workspace);
        return getPaginated(uri, RepositoryResponse.class);
    }

    /**
     * Get the current user's access right on the given repository.
     *
     * @param repository
     *            name
     * @return the privilege of this user on the repository
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public Permission getPermission(String repository) throws ClientException {
        URI requestURI = UriBuilder.fromUri(USER_PERMISSIONS) //
                .queryParam(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .queryParam(QUERY_PARAM_QUERY, "repository.name=\"" + repository + "\"") //
                .build();
        List<UserPermission> permissions = getPaginated(requestURI, UserPermissionResponse.class);
        Permission higher = Permission.NONE;
        for (UserPermission p : permissions) {
            if (p.getPrivilege().ordinal() > higher.ordinal()) {
                higher = p.getPrivilege();
            }
        }
        return higher;
    }

    /**
     * Find groups which doesn't have access rights to the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param workspace
     *            name
     * @param repository
     *            name
     * @return all the existing groups that doesn't have access rights to the
     *         repository
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public List<String> findAvailableGroups(String workspace, String repository) throws ClientException {
        URI requestURI = UriBuilder.fromUri(GROUP_NO_API_INTERNAL).build(workspace, repository);
        try (Response response = get(createBuilder(requestURI))) {
            List<GroupInfo> groups = response.readEntity(new GenericType<List<GroupInfo>>() {});

            return groups.stream().map(GroupInfo::getSlug).collect(Collectors.toList());
        }
    }

    /**
     * Find groups which have access rights to the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param workspace
     *            name
     * @param repository
     *            the repository name
     * @return the groups that have access right to the repository, with their
     *         privilege
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public Map<String, Permission> getGroupsPermission(String workspace, String repository) throws ClientException {
        URI requestURI = UriBuilder.fromUri(GROUP_NO_API) //
                .queryParam(QUERY_PARAM_EXCLUDE_MEMBERS, "1") //
                .build(workspace, repository);
        // Each group associated to its privilege
        try (Response response = get(createBuilder(requestURI))) {
            List<GroupPermissionResponse> data = response.readEntity(new GenericType<List<GroupPermissionResponse>>() {});

            return data.stream() //
                    .collect(Collectors.toMap(group -> group.getGroup().getSlug(), GroupPermissionResponse::getPrivilege));
        }
    }

    /**
     * Find users which have single access rights to the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param repository
     *            the repository name
     * @param filter
     *            username that start with
     * @return the groups that have access right to the repository, with their
     *         privilege
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public List<UserInfo> findUser(String workspace, String repository, String filter) throws ClientException {
        URI requestURI = UriBuilder.fromUri(USER_NO_API_INTERNAL) //
                .queryParam(QUERY_PARAM_HAS_ACCESS, false) //
                .queryParam(QUERY_PARAM_TERM, filter) //
                .build(workspace, repository);
        try (Response response = get(createBuilder(requestURI))) {
            return response.readEntity(new GenericType<List<UserInfo>>() {});
        }
    }

    /**
     * Returns branch restriction for the given repository.
     *
     * @param workspace
     *            name
     * @param repository
     *            the repository name
     * @return the list of branch restriction setup for this repository
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public List<BranchRestriction> getBranchRestrictions(String workspace, String repository) throws ClientException {
        URI uri = UriBuilder.fromUri(REPOSITORY_BRANCH_RESTRICTIONS) //
                .queryParam(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .build(workspace, repository);
        return getPaginated(uri, BranchPermissionResponse.class);
    }

    /**
     * Grant access right to a group or update the current group's privilege to
     * the given repository.
     * <p>
     * This API is not available with access token credentials.
     *
     * @param workspace
     *            name
     * @param repository
     *            the repository name
     * @param groupName
     *            the group name
     * @param accessLevel
     *            read, write or admin access level
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public void updateGroupAccess(String workspace, String repository, String groupName, Permission accessLevel) throws ClientException {
        URI requestURI = UriBuilder.fromUri(GROUP_API_V1).build(workspace, repository, workspace, groupName);

        String permission = accessLevel.toString().toLowerCase();
        try (Response response = put(createBuilder(requestURI), permission, MediaType.TEXT_PLAIN_TYPE)) {
            // NOSONAR
        }
    }

    /**
     * Get user right on a specified repository.
     *
     * @param workspace
     *            name
     * @param repository
     *            the repository name
     * @param username
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public UserPermission getUserPermission(String workspace, String repository, String username) throws ClientException {
        URI requestURI = UriBuilder.fromUri(PERMISSIONS) //
                .queryParam(QUERY_PARAM_QUERY, "user.nickname=\"{username}\"") //
                .build(workspace, repository, username);
        try (Response response = get(createBuilder(requestURI))) {
            UserPermissionResponse data = response.readEntity(UserPermissionResponse.class);
            if (data.getValues().isEmpty()) {
                UserPermission none = new UserPermission();
                none.setPrivilege(Permission.NONE);
                return none;
            }
            return data.getValues().get(0);
        }
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
     * @param workspace
     *            name
     * @param repository
     *            the repository name
     * @param groupName
     *            the group name
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public void deleteGroupAccess(String workspace, String repository, String groupName) throws ClientException {
        URI requestURI = UriBuilder.fromUri(GROUP_API_V1).build(workspace, repository, workspace, groupName);
        try (Response response = delete(createBuilder(requestURI))) {
            // NOSONAR
        }
    }

    /**
     * Add or update the given branch permission for the specified repository.
     *
     * @param workspace
     *            bitbucket
     * @param repository
     *            the repository name
     * @param permission
     *            to add or update
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public void updateBranchRestriction(String workspace, String repository, BranchRestriction permission) throws ClientException {
        UriBuilder requestBuilder = UriBuilder.fromUri(REPOSITORY_BRANCH_RESTRICTIONS);

        if (permission.getId() != null) {
            URI requestURI = requestBuilder.path("{id}").build(workspace, repository, permission.getId());
            Builder builder = createBuilder(requestURI);

            try (Response response = put(builder, permission)) {
                // NOSONAR
            }
        } else {
            URI requestURI = requestBuilder.build(workspace, repository);
            Builder builder = createBuilder(requestURI);

            try (Response response = post(builder, permission)) {
                // NOSONAR
            }
        }
    }

    /**
     * Gets all repository web hooks.
     *
     * @param workspace
     *            bitbucket
     * @param repository
     *            the repository name
     * @param hookName
     *            a filter based on partial match of the web hook name
     * @return list of web hook
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public List<Webhook> getWebhooks(String workspace, String repository, String... hookName) throws ClientException {
        List<Webhook> webhooks = new LinkedList<>();

        URI uri = UriBuilder.fromUri(REPOSITORY_WEBHOOKS) //
                .queryParam(QUERY_PARAM_PAGELEN, DEFAULT_PAGE_LEN) //
                .build(workspace, repository);
        webhooks = getPaginated(uri, WebhookResponse.class);

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
     * @param workspace
     *            bitbucket
     * @param repository
     *            the repository name
     * @param webhook
     *            name
     * @return created web hook
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public Webhook updateWebhook(String workspace, String repository, Webhook webhook) throws ClientException {
        URI uri = UriBuilder.fromUri(REPOSITORY_WEBHOOKS_ID) //
                .build(workspace, repository, webhook.getUUID());
        try (Response response = put(createBuilder(uri), webhook)) {
            return response.readEntity(Webhook.class);
        }
    }

    /**
     * Deletes web hook.
     *
     * @param workspace
     *            bitbucket
     * @param repository
     *            the repository name
     * @param webhookId
     *            web hook identifier
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public void deleteWebhook(String workspace, String repository, String webhookId) throws ClientException {
        URI uri = UriBuilder.fromUri(REPOSITORY_WEBHOOKS).build(workspace, repository, webhookId);
        try (Response response = delete(createBuilder(uri))) {
        }
    }

    /**
     * Creates a new web hook in the specified repository.
     *
     * @param workspace
     *            bitbucket
     * @param repository
     *            the repository name
     * @param webhook
     *            to create
     * @return created web hook
     * @throws ClientException
     *             in case of HTTP response from server different than 20x codes
     */
    public Webhook addWebHook(String workspace, String repository, Webhook webhook) throws ClientException {
        URI requestURI = UriBuilder.fromUri(REPOSITORY_WEBHOOKS).build(workspace, repository);
        try (Response response = post(createBuilder(requestURI), webhook)) {
            return response.readEntity(Webhook.class);
        }
    }

    private Builder createBuilder(URI requestURI) throws ClientException {
        if (hasOAuth() && authToken == null) {
            LocalDateTime now = LocalDateTime.now();
            authToken = oauth2Grant();
            tokenExpiration = now.plusSeconds(authToken.getExpiry());
            client.register(OAuth2ClientSupport.feature(authToken.getAccessToken()));
        }
        return client.target(requestURI) //
                .request(MediaType.APPLICATION_JSON) //
                .accept(MediaType.APPLICATION_JSON) //
                .header(HEADER_CSRF, "no-check");
    }

    private void refreshToken() {
        String auth = getBasicAuth(credentials);

        URI uri = UriBuilder.fromUri(OAUTH2).build();
        Builder request = client.target(uri) //
                .request() //
                .header(HEADER_AUTHORIZATION, auth);

        Form form = new Form(FORM_PARAM_GRANT_TYPE, GRANT_TYPE_REFRESH);
        form.param(FORM_PARAM_REFRESH_TOKEN, authToken.getRefreshToken());

        try (Response response = call(() -> request.post(Entity.form(form)))) {
            authToken = response.readEntity(AuthToken.class);
            tokenExpiration = LocalDateTime.now().plusSeconds(authToken.getExpiry());
        } catch (Exception e) {
            authToken = null;
        }
    }

    private AuthToken oauth2Grant() throws ClientException {
        String auth = getBasicAuth(credentials);

        URI uri = UriBuilder.fromUri(OAUTH2).build();
        Builder request = client.target(uri) //
                .request() //
                .header(HEADER_AUTHORIZATION, auth);

        Form form = new Form(FORM_PARAM_GRANT_TYPE, GRANT_TYPE_CC);
        Response response = call(() -> request.post(Entity.form(form)));
        return response.readEntity(AuthToken.class);
    }

    private String getBasicAuth(Credentials credentials) {
        return "Basic " + Base64.getEncoder().encodeToString((credentials.getUser() + ":"
                + credentials.getPassword()).getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasOAuth() {
        return credentials instanceof OAuth2Consumer;
    }

    private boolean isTokenExpired() {
        return hasOAuth() && authToken != null && LocalDateTime.now().isAfter(tokenExpiration);
    }

    protected void buildClient() {
        ClientConfig config = new ClientConfig(jaxbProvider());
        client = ClientBuilder.newClient(config);

        if (!hasOAuth()) {
            client.register(HttpAuthenticationFeature.basic(credentials.getUser(), credentials.getPassword()));
        }
    }

    protected JacksonJaxbJsonProvider jaxbProvider() {
        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return jacksonProvider;
    }

    @Override
    public void close() {
        client.close();
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

}
