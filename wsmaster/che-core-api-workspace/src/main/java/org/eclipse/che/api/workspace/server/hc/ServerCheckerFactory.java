/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.server.hc;

import java.net.URL;
import java.util.Timer;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;

/**
 * Creates {@link HttpConnectionServerChecker} for server readiness checking.
 *
 * @author Alexander Garagatyi
 */
public interface ServerCheckerFactory {
  HttpConnectionServerChecker httpChecker(
      URL url, RuntimeIdentity runtimeIdentity, String machineName, String serverRef, Timer timer)
      throws InfrastructureException;
}
