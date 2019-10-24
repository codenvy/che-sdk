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
package org.eclipse.che.api.workspace.server;

import java.util.Map;
import org.eclipse.che.api.core.ValidationException;

/**
 * Adds an ability to extends logic of workspace attributes validation.
 *
 * <p>It may needed since attributes may be used as configuration storage by different components.
 * And that components may need to validate attributes.
 *
 * @author Sergii Leshchenko
 */
public interface WorkspaceAttributeValidator {

  /**
   * Validates if the specified workspace attributes does not contain invalid attributes according
   * to implementors rules.
   *
   * @param attributes workspace attributes to validate
   * @throws ValidationException when the specified workspace attributes is not valid
   */
  void validate(Map<String, String> attributes) throws ValidationException;
}
