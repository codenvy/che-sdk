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
package org.eclipse.che.api.factory.server.urlfactory;

import java.util.List;
import java.util.Optional;

/**
 * Provides basic information about the remote factory URL components. Vendor specific
 * implementations may provide wider range of details about the URL (like username, repository etc).
 */
public interface RemoteFactoryUrl {

  /**
   * Filename of factory json (typically .factory.json) or {@code null} if it cannot be clearly
   * detected from provided factory url.
   *
   * @return factory json filename
   */
  String getFactoryFilename();

  /**
   * Location of factory json. Must points to the raw factory file content.
   *
   * @return factory json file location
   */
  String factoryFileLocation();

  /**
   * List of possible filenames and raw locations of devfile.
   *
   * @return devfile filenames and locations list
   */
  List<DevfileLocation> devfileFileLocations();

  /** Describes devfile location, including filename if any. */
  interface DevfileLocation {
    Optional<String> filename();

    String location();
  }
}
