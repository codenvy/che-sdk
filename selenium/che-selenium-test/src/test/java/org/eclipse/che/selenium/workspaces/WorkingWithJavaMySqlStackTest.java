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
package org.eclipse.che.selenium.workspaces;

import static org.eclipse.che.selenium.core.constant.TestBuildConstants.BUILD_SUCCESS;
import static org.eclipse.che.selenium.core.constant.TestStacksConstants.JAVA_MYSQL;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.APPLICATION_START_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.LOADER_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.LOAD_PAGE_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.PREPARING_WS_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.UPDATING_PROJECT_TIMEOUT_SEC;
import static org.eclipse.che.selenium.pageobject.Consoles.CommandsGoal.COMMON;
import static org.eclipse.che.selenium.pageobject.Consoles.CommandsGoal.RUN;
import static org.eclipse.che.selenium.pageobject.dashboard.NavigationBar.MenuItem.WORKSPACES;
import static org.openqa.selenium.Keys.ENTER;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.client.TestWorkspaceServiceClient;
import org.eclipse.che.selenium.core.user.TestUser;
import org.eclipse.che.selenium.pageobject.AskDialog;
import org.eclipse.che.selenium.pageobject.Consoles;
import org.eclipse.che.selenium.pageobject.Loader;
import org.eclipse.che.selenium.pageobject.ProjectExplorer;
import org.eclipse.che.selenium.pageobject.dashboard.CreateWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.DashboardWorkspace;
import org.eclipse.che.selenium.pageobject.dashboard.NavigationBar;
import org.eclipse.che.selenium.pageobject.dashboard.ProjectSourcePage;
import org.eclipse.che.selenium.pageobject.machineperspective.MachineTerminal;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/** @author Aleksandr Shmaraev */
public class WorkingWithJavaMySqlStackTest {
  private static final String WORKSPACE = NameGenerator.generate("java-mysql", 4);
  private static final String PROJECT_NAME = "web-java-petclinic";
  private static final String BUIL_AND_DEPLOY_PROCESS = PROJECT_NAME + ":build and deploy";

  private static final List<String> infoDataBases =
      Arrays.asList("Database", "information_schema", "petclinic", "mysql");
  private static final String MSG_CLOSE_PROCESS =
      String.format(
          "The process %s:build and deploy will be terminated after closing console. Do you want to continue?",
          PROJECT_NAME);

  @Inject private TestUser defaultTestUser;
  @Inject private ProjectExplorer projectExplorer;
  @Inject private Loader loader;
  @Inject private Consoles consoles;
  @Inject private NavigationBar navigationBar;
  @Inject private CreateWorkspace createWorkspace;
  @Inject private ProjectSourcePage projectSourcePage;
  @Inject private Dashboard dashboard;
  @Inject private DashboardWorkspace dashboardWorkspace;
  @Inject private AskDialog askDialog;
  @Inject private MachineTerminal terminal;
  @Inject private SeleniumWebDriver seleniumWebDriver;
  @Inject private TestWorkspaceServiceClient workspaceServiceClient;

  @AfterClass
  public void tearDown() throws Exception {
    workspaceServiceClient.delete(WORKSPACE, defaultTestUser.getName());
  }

  @Test
  public void checkJavaMySqlAndRunApp() {
    String currentWindow;

    // create a workspace from the Java-MySql stack with the web-java-petclinic project
    dashboard.open();
    navigationBar.waitNavigationBar();
    navigationBar.clickOnMenu(WORKSPACES);
    dashboardWorkspace.waitToolbarTitleName("Workspaces");
    dashboardWorkspace.clickOnNewWorkspaceBtn();
    createWorkspace.waitToolbar();
    createWorkspace.selectStack(JAVA_MYSQL.getId());
    createWorkspace.typeWorkspaceName(WORKSPACE);
    projectSourcePage.clickAddOrImportProjectButton();
    projectSourcePage.selectSample(PROJECT_NAME);
    projectSourcePage.clickAdd();
    createWorkspace.clickCreate();

    seleniumWebDriver.switchFromDashboardIframeToIde(LOADER_TIMEOUT_SEC);
    currentWindow = seleniumWebDriver.getWindowHandle();
    projectExplorer.waitProjectExplorer();
    projectExplorer.waitItem(PROJECT_NAME, APPLICATION_START_TIMEOUT_SEC);
    projectExplorer.selectItem(PROJECT_NAME);

    // Select the db machine and perform 'show databases'
    consoles.startCommandFromProcessesArea("db", COMMON, "show databases");
    consoles.waitTabNameProcessIsPresent("db");
    for (String text : infoDataBases) {
      consoles.waitExpectedTextIntoConsole(text);
    }

    // Build and deploy the web application
    consoles.startCommandFromProcessesArea("dev-machine", RUN, BUIL_AND_DEPLOY_PROCESS);
    consoles.waitTabNameProcessIsPresent(BUIL_AND_DEPLOY_PROCESS);
    consoles.waitProcessInProcessConsoleTree(BUIL_AND_DEPLOY_PROCESS);
    consoles.waitExpectedTextIntoConsole(BUILD_SUCCESS, UPDATING_PROJECT_TIMEOUT_SEC);
    consoles.waitExpectedTextIntoConsole("Server startup in", PREPARING_WS_TIMEOUT_SEC);
    consoles.waitPreviewUrlIsPresent();

    // Run the application
    consoles.clickOnPreviewUrl();
    seleniumWebDriver.switchToNoneCurrentWindow(currentWindow);
    checkWebJavaPetclinicAppl();
    seleniumWebDriver.close();
    seleniumWebDriver.switchTo().window(currentWindow);
    seleniumWebDriver.switchFromDashboardIframeToIde();

    // Close terminal tab for 'build and deploy' process
    consoles.waitProcessInProcessConsoleTree(BUIL_AND_DEPLOY_PROCESS);
    consoles.waitTabNameProcessIsPresent(BUIL_AND_DEPLOY_PROCESS);
    consoles.closeProcessByTabName(BUIL_AND_DEPLOY_PROCESS);
    askDialog.acceptDialogWithText(MSG_CLOSE_PROCESS);
    consoles.waitProcessIsNotPresentInProcessConsoleTree(BUIL_AND_DEPLOY_PROCESS);
    consoles.waitTabNameProcessIsNotPresent(BUIL_AND_DEPLOY_PROCESS);

    // Check that tomcat is not running
    consoles.selectProcessByTabName("Terminal");
    loader.waitOnClosed();
    terminal.typeIntoTerminal("ps ax | grep tomcat8");
    terminal.typeIntoTerminal(ENTER.toString());
    terminal.waitExpectedTextNotPresentTerminal("catalina.startup.Bootstrap start");
  }

  /** check main elements of the web-java-petclinic */
  private void checkWebJavaPetclinicAppl() {
    new WebDriverWait(seleniumWebDriver, LOADER_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(By.xpath("//h2[text()='Welcome']")));
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(visibilityOfElementLocated(By.xpath("//div[@class='navbar-inner']")));
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(presenceOfElementLocated(By.xpath("//table[@class='footer']")));
  }
}
