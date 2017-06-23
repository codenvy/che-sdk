/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.workspace.infrastructure.docker.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Server checker that uses http connection response code as a criteria of availability of a server.
 * </p>
 * If response code is not less than 200 and less than 400 server is treated as available.
 *
 * @author Alexander Garagatyi
 */
public class HttpConnectionServerChecker extends ServerChecker {
    private final URL url;

    public HttpConnectionServerChecker(URL url,
                                       String machineName,
                                       String serverRef,
                                       long period,
                                       long timeout,
                                       TimeUnit timeUnit) {
        super(machineName, serverRef, period, timeout, timeUnit);
        this.url = url;
    }

    @Override
    public boolean isAvailable() {
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection)url.openConnection();
            // TODO consider how much time we should use as a limit
            httpURLConnection.setConnectTimeout((int)TimeUnit.SECONDS.toMillis(3));
            httpURLConnection.setReadTimeout((int)TimeUnit.SECONDS.toMillis(3));
            return isConnectionSuccessful(httpURLConnection);
        } catch (IOException e) {
            return false;
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    boolean isConnectionSuccessful(HttpURLConnection conn) {
        try {
            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (IOException e) {
            return false;
        }
    }
}
