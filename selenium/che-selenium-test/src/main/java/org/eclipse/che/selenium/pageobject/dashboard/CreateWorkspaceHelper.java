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

package org.eclipse.che.selenium.pageobject.dashboard;

import com.google.inject.Inject;
import java.util.ArrayList;
import org.eclipse.che.selenium.pageobject.dashboard.workspaces.Workspaces;

/** @author Skoryk Serhii */
public class CreateWorkspaceHelper {

  @Inject private Dashboard dashboard;
  @Inject private Workspaces workspaces;
  @Inject private NewWorkspace newWorkspace;
  @Inject private ProjectSourcePage projectSourcePage;

  public void createWorkspaceFromStackWithProject(
      NewWorkspace.Stack stack, String workspaceName, String projectName) {
    createWorkspace(stack, workspaceName);

    projectSourcePage.clickOnAddOrImportProjectButton();
    projectSourcePage.selectSample(projectName);
    projectSourcePage.clickOnAddProjectButton();

    newWorkspace.clickOnCreateButtonAndOpenInIDE();
  }

  public void createWorkspaceFromStackWithoutProject(
      NewWorkspace.Stack stack, String workspaceName) {
    createWorkspace(stack, workspaceName);

    newWorkspace.clickOnCreateButtonAndOpenInIDE();
  }

  public void createWorkspaceFromStackWithProjects(
      NewWorkspace.Stack stack, String workspaceName, ArrayList<String> projectNames) {
    createWorkspace(stack, workspaceName);

    projectSourcePage.clickOnAddOrImportProjectButton();

    projectNames.forEach(
        project -> {
          projectSourcePage.selectSample(project);
        });

    projectSourcePage.clickOnAddProjectButton();
    newWorkspace.clickOnCreateButtonAndOpenInIDE();
  }

  private void createWorkspace(NewWorkspace.Stack stack, String workspaceName) {
    dashboard.waitDashboardToolbarTitle();

    dashboard.selectWorkspacesItemOnDashboard();
    workspaces.clickOnAddWorkspaceBtn();

    newWorkspace.waitToolbar();
    newWorkspace.clickOnAllStacksTab();
    newWorkspace.selectStack(stack);
    newWorkspace.typeWorkspaceName(workspaceName);
  }
}
