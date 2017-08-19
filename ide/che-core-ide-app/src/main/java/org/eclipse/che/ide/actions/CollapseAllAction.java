/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.actions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

/** @author Vlad Zhukovskiy */
@Singleton
public class CollapseAllAction extends ProjectAction {

  private ProjectExplorerPresenter projectExplorer;

  @Inject
  public CollapseAllAction(ProjectExplorerPresenter projectExplorer) {
    super("Collapse All");
    this.projectExplorer = projectExplorer;
  }

  @Override
  protected void updateProjectAction(ActionEvent e) {
    //stub
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    projectExplorer.collapseAll();
  }
}
