/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.factory.server.gitlab;

import java.util.Optional;
import org.eclipse.che.api.factory.server.scm.AuthorizingFileContentProvider;
import org.eclipse.che.api.factory.server.scm.GitCredentialManager;
import org.eclipse.che.api.factory.server.scm.PersonalAccessToken;
import org.eclipse.che.api.factory.server.scm.PersonalAccessTokenManager;
import org.eclipse.che.api.factory.server.scm.ScmAuthenticationToken;
import org.eclipse.che.api.factory.server.scm.exception.ScmConfigurationPersistenceException;
import org.eclipse.che.api.factory.server.scm.exception.ScmUnauthorizedException;
import org.eclipse.che.api.workspace.server.devfile.URLFetcher;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileException;
import org.eclipse.che.commons.env.EnvironmentContext;

/** Gitlab specific authorizing file content provider. */
class GitlabAuthorizingFileContentProvider extends AuthorizingFileContentProvider<GitlabUrl> {

  private final PersonalAccessTokenManager personalAccessTokenManager;
  private final GitlabApiClient gitlabApiClient;

  GitlabAuthorizingFileContentProvider(
      GitlabUrl githubUrl,
      URLFetcher urlFetcher,
      GitCredentialManager gitCredentialManager,
      PersonalAccessTokenManager personalAccessTokenManager,
      GitlabApiClient gitlabApiClient) {
    super(githubUrl, urlFetcher, gitCredentialManager);
    this.personalAccessTokenManager = personalAccessTokenManager;
    this.gitlabApiClient = gitlabApiClient;
  }

  @Override
  protected ScmAuthenticationToken getScmAuthenticationToken(String requestURL)
      throws DevfileException {
    try {
      Optional<PersonalAccessToken> token =
          personalAccessTokenManager.get(
              EnvironmentContext.getCurrent().getSubject(), remoteFactoryUrl.getHostName());

      if (token.isPresent()) {
        return token.get();
      } else {
        return gitlabApiClient.getOAuthToken(
            EnvironmentContext.getCurrent().getSubject(), remoteFactoryUrl.getHostName());
      }
    } catch (ScmUnauthorizedException | ScmConfigurationPersistenceException e) {
      throw new DevfileException(e.getMessage(), e);
    }
  }
}
