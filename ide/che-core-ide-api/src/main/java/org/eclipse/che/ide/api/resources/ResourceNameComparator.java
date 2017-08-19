/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.api.resources;

import com.google.common.annotations.Beta;
import java.util.Comparator;

/**
 * Compares two {@link Resource} by their name ignoring case.
 *
 * @author Vlad Zhukovskiy
 * @see Resource#getName()
 * @since 4.4.0
 */
@Beta
public class ResourceNameComparator implements Comparator<Resource> {

  private static final ResourceNameComparator INSTANCE = new ResourceNameComparator();

  private ResourceNameComparator() {}

  public static Comparator<Resource> getInstance() {
    return INSTANCE;
  }

  /** {@inheritDoc} */
  @Override
  public int compare(Resource o1, Resource o2) {
    return o1.getName().compareToIgnoreCase(o2.getName());
  }
}
