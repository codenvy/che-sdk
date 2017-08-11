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
package org.eclipse.che.api.installer.server;

import com.google.inject.Provider;

import org.eclipse.che.api.installer.shared.model.Installer;

import java.util.List;

/**
 * Installers provider.
 *
 * @author Anatolii Bazko
 */
public interface InstallerProvider extends Provider<List<Installer>> {
}
