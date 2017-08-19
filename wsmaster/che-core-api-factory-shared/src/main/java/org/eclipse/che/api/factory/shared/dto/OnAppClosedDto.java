/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.api.factory.shared.dto;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

import java.util.List;
import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.core.model.factory.OnAppClosed;
import org.eclipse.che.dto.shared.DTO;

/**
 * Describe IDE look and feel on application closed event.
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface OnAppClosedDto extends OnAppClosed {

  /** @return actions for current event. */
  @Override
  @FactoryParameter(obligation = OPTIONAL)
  List<IdeActionDto> getActions();

  void setActions(List<IdeActionDto> actions);

  OnAppClosedDto withActions(List<IdeActionDto> actions);
}
