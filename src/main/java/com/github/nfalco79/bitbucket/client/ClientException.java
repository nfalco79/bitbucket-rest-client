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

/**
 * Exception raised by BBClient.
 */
@SuppressWarnings("serial")
public class ClientException extends IOException {

    /**
     * Create an exception with the given message.
     *
     * @param message
     *            the exception message
     */
    public ClientException(String message) {
        super(message);
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

}