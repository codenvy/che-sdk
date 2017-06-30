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
package org.eclipse.che.api.installer.server.exception;

import org.eclipse.che.api.core.BadRequestException;

/**
 * @author Sergii Leshchenko
 */
public class IllegalInstallerKey extends RuntimeException {
    public IllegalInstallerKey(String message) {
        super(message);
    }

    public IllegalInstallerKey(String message, BadRequestException cause) {
        super(message, cause);
    }
}
