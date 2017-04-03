/*******************************************************************************
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.pullrequest.client.rest;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.StringMapUnmarshaller;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

public class PullRequestWorkflowServiceClientImpl implements PullRequestWorkflowServiceClient {
    protected AsyncRequestFactory asyncRequestFactory;
    protected String              baseHttpUrl;

    @Inject
    public PullRequestWorkflowServiceClientImpl(AppContext appContext, AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.baseHttpUrl = appContext.getMasterEndpoint() + "/pullrequestwf";
    }

    @Override
    public Promise<Map<String, String>> getSettings() {
        return asyncRequestFactory.createGetRequest(baseHttpUrl + "/settings") //
                                  .header(ACCEPT, APPLICATION_JSON) //
                                  .header(CONTENT_TYPE, APPLICATION_JSON) //
                                  .send(new StringMapUnmarshaller());
    }
}
