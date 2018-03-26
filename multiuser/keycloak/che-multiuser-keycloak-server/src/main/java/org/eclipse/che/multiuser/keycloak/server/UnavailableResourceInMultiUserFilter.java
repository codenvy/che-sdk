/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.keycloak.server;

import java.io.IOException;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter that will return HTTP status 403. Used for resources, that are not meant to be available
 * in multi-user Che. Filter omits GET requests.
 */
@Singleton
public class UnavailableResourceInMultiUserFilter implements Filter {
  protected static final String ERROR_RESPONSE_JSON_MESSAGE =
      "{\"error\" : \"This resource is unavailable in multi-user Che\" }";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String requestMethod = ((HttpServletRequest) request).getMethod();
    if (requestMethod.equals("GET")) {
      // allow request to go through
      chain.doFilter(request, response);
    }

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(403);
    httpResponse.setContentType("application/json");
    httpResponse.getWriter().println(ERROR_RESPONSE_JSON_MESSAGE);
  }

  @Override
  public void destroy() {}
}
