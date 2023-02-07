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

import java.io.IOException;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * Exception raised by {@link BitbucketCloudClient}.
 *
 * @author Nikolas Falco
 */
@SuppressWarnings("serial")
public class ClientException extends IOException {

    private int status;
    private String response;

    /**
     * Create an exception with the given message.
     *
     * @param response
     *            the client response error
     */
    public ClientException(ClassicHttpResponse response) {
        super("HTTP " + response.getCode());
        this.status = response.getCode();
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                this.response = EntityUtils.toString(entity);
            }
        } catch (ParseException | IOException e) {
        }
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        return (response != null) ? message + ": " + response : message;
    }

    /**
     * Create an exception with the given message.
     *
     * @param message
     *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is permitted, and indicates that the
     *            cause is nonexistent or unknown.)
     * @since 1.5
     */
    protected ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getStatus() {
        return status;
    }

}