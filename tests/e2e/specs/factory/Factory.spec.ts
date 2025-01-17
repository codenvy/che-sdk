/** *******************************************************************
 * copyright (c) 2021-2024 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import 'reflect-metadata';

import { e2eContainer } from '../../configs/inversify.config';
import {
	ActivityBar,
	ContextMenu,
	EditorView,
	Locators,
	NewScmView,
	SingleScmProvider,
	TextEditor,
	ViewControl,
	ViewSection
} from 'monaco-page-objects';
import { expect } from 'chai';
import { OauthPage } from '../../pageobjects/git-providers/OauthPage';
import { StringUtil } from '../../utils/StringUtil';
import { CheCodeLocatorLoader } from '../../pageobjects/ide/CheCodeLocatorLoader';
import { registerRunningWorkspace } from '../MochaHooks';
import { BrowserTabsUtil } from '../../utils/BrowserTabsUtil';
import { WorkspaceHandlingTests } from '../../tests-library/WorkspaceHandlingTests';
import { CLASSES, TYPES } from '../../configs/inversify.types';
import { DriverHelper } from '../../utils/DriverHelper';
import { ProjectAndFileTests } from '../../tests-library/ProjectAndFileTests';
import { Logger } from '../../utils/Logger';
import { LoginTests } from '../../tests-library/LoginTests';
import { OAUTH_CONSTANTS } from '../../constants/OAUTH_CONSTANTS';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';
import { FACTORY_TEST_CONSTANTS } from '../../constants/FACTORY_TEST_CONSTANTS';
import { ITestWorkspaceUtil } from '../../utils/workspace/ITestWorkspaceUtil';
import { Dashboard } from '../../pageobjects/dashboard/Dashboard';
import { CreateWorkspace } from '../../pageobjects/dashboard/CreateWorkspace';
import { ViewsMoreActionsButton } from '../../pageobjects/ide/ViewsMoreActionsButton';

suite(
	`Create a workspace via launching a factory from the ${FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_GIT_PROVIDER} repository ${BASE_TEST_CONSTANTS.TEST_ENVIRONMENT}`,
	function (): void {
		const browserTabsUtil: BrowserTabsUtil = e2eContainer.get(CLASSES.BrowserTabsUtil);
		const workspaceHandlingTests: WorkspaceHandlingTests = e2eContainer.get(CLASSES.WorkspaceHandlingTests);
		const projectAndFileTests: ProjectAndFileTests = e2eContainer.get(CLASSES.ProjectAndFileTests);
		const driverHelper: DriverHelper = e2eContainer.get(CLASSES.DriverHelper);
		const loginTests: LoginTests = e2eContainer.get(CLASSES.LoginTests);
		const cheCodeLocatorLoader: CheCodeLocatorLoader = e2eContainer.get(CLASSES.CheCodeLocatorLoader);
		const webCheCodeLocators: Locators = cheCodeLocatorLoader.webCheCodeLocators;
		const oauthPage: OauthPage = e2eContainer.get(CLASSES.OauthPage);
		const testWorkspaceUtil: ITestWorkspaceUtil = e2eContainer.get(TYPES.WorkspaceUtil);
		const dashboard: Dashboard = e2eContainer.get(CLASSES.Dashboard);
		const createWorkspace: CreateWorkspace = e2eContainer.get(CLASSES.CreateWorkspace);
		const viewsMoreActionsButton: ViewsMoreActionsButton = e2eContainer.get(CLASSES.ViewsMoreActionsButton);

		let projectSection: ViewSection;
		let scmProvider: SingleScmProvider;
		let rest: SingleScmProvider[];
		let scmContextMenu: ContextMenu;
		let viewsActionsButton: boolean;

		// test specific data
		const timeToRefresh: number = 1500;
		const changesToCommit: string = new Date().getTime().toString();
		const fileToChange: string = 'Date.txt';
		const refreshButtonLabel: string = 'Refresh';
		const pushItemLabel: string = 'Push';
		let testRepoProjectName: string;

		suiteSetup('Login', async function (): Promise<void> {
			await loginTests.loginIntoChe();
		});
		test('Navigate to the factory URL', async function (): Promise<void> {
			await browserTabsUtil.navigateTo(FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_URL());
			await createWorkspace.performTrustAuthorPopup();
		});

		if (OAUTH_CONSTANTS.TS_SELENIUM_GIT_PROVIDER_OAUTH) {
			test(`Authorize with a ${FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_GIT_PROVIDER} OAuth`, async function (): Promise<void> {
				await oauthPage.login();
				await oauthPage.waitOauthPage();
				await oauthPage.confirmAccess();
			});
		}

		if (BASE_TEST_CONSTANTS.IS_RESTART_EXISTING_WORKSPACE) {
			test('Restart existing workspace', async function (): Promise<void> {
				await dashboard.waitExistingWorkspaceFoundAlert();
				await dashboard.clickOpenExistingWorkspaceButton();
			});
		}

		test('Obtain workspace name from workspace loader page', async function (): Promise<void> {
			await workspaceHandlingTests.obtainWorkspaceNameFromStartingPage();
		});

		test('Registering the running workspace', function (): void {
			registerRunningWorkspace(WorkspaceHandlingTests.getWorkspaceName());
		});

		test('Wait the workspace readiness', async function (): Promise<void> {
			await projectAndFileTests.waitWorkspaceReadinessForCheCodeEditor();
		});

		test('Check if a project folder has been created', async function (): Promise<void> {
			testRepoProjectName = StringUtil.getProjectNameFromGitUrl(FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_GIT_REPO_URL);
			Logger.debug(`new SideBarView().getContent().getSection: get ${testRepoProjectName}`);
			projectSection = await projectAndFileTests.getProjectViewSession();
			expect(await projectAndFileTests.getProjectTreeItem(projectSection, testRepoProjectName), 'Project folder was not imported').not
				.undefined;
		});

		test('Accept the project as a trusted one', async function (): Promise<void> {
			await projectAndFileTests.performTrustAuthorDialog();
		});

		test('Check if the project files were imported', async function (): Promise<void> {
			const label: string = BASE_TEST_CONSTANTS.TS_SELENIUM_PROJECT_ROOT_FILE_NAME;
			expect(await projectAndFileTests.getProjectTreeItem(projectSection, label), 'Project files were not imported').not.undefined;
		});

		test('Make changes to the file', async function (): Promise<void> {
			Logger.debug(`projectSection.openItem: "${fileToChange}"`);
			await projectSection.openItem(testRepoProjectName, fileToChange);
			const editor: TextEditor = (await new EditorView().openEditor(fileToChange)) as TextEditor;
			await driverHelper.waitVisibility(webCheCodeLocators.Editor.inputArea);
			Logger.debug('editor.clearText');
			await editor.clearText();
			Logger.debug(`editor.typeTextAt: "${changesToCommit}"`);
			await editor.typeTextAt(1, 1, changesToCommit);
		});

		test('Open a source control manager', async function (): Promise<void> {
			const viewSourceControl: string = 'Source Control';
			const sourceControl: ViewControl = (await new ActivityBar().getViewControl(viewSourceControl)) as ViewControl;
			Logger.debug(`sourceControl.openView: "${viewSourceControl}"`);
			await sourceControl.openView();
			const scmView: NewScmView = new NewScmView();
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.inputField);
			[scmProvider, ...rest] = await scmView.getProviders();
			if (scmProvider === undefined) {
				await projectAndFileTests.performManageWorkspaceTrustBox();
				[scmProvider, ...rest] = await scmView.getProviders();
			}
			Logger.debug(`scmView.getProviders: "${JSON.stringify(scmProvider)}, ${rest}"`);
		});

		test('Check if the changes are displayed in the source control manager', async function (): Promise<void> {
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.more);
			await driverHelper.wait(timeToRefresh);
			Logger.debug(`wait and click on: "${refreshButtonLabel}"`);
			await driverHelper.waitAndClick(webCheCodeLocators.ScmView.actionConstructor(refreshButtonLabel));
			// wait while changes counter will be refreshed
			await driverHelper.wait(timeToRefresh);
			const changes: number = await scmProvider.getChangeCount();
			Logger.debug(`scmProvider.getChangeCount: number of changes is "${changes}"`);
			expect(changes).eql(1);
		});

		test('Stage the changes', async function (): Promise<void> {
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.more);
			viewsActionsButton = await viewsMoreActionsButton.viewsAndMoreActionsButtonIsVisible();
			if (viewsActionsButton) {
				await viewsMoreActionsButton.closeSourceControlGraph();
			}
			Logger.debug('scmProvider.openMoreActions');
			scmContextMenu = await scmProvider.openMoreActions();
			await driverHelper.waitVisibility(webCheCodeLocators.ContextMenu.contextView);
			Logger.debug('scmContextMenu.select: "Changes" -> "Stage All Changes"');
			await scmContextMenu.select('Changes', 'Stage All Changes');
		});

		test('Commit the changes', async function (): Promise<void> {
			Logger.debug(`scmProvider.commitChanges: commit name "Commit ${changesToCommit}"`);
			await scmProvider.commitChanges('Commit ' + changesToCommit);
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.more);
			await driverHelper.wait(timeToRefresh);
			Logger.debug(`wait and click on: "${refreshButtonLabel}"`);
			await driverHelper.waitAndClick(webCheCodeLocators.ScmView.actionConstructor(refreshButtonLabel));
			// wait while changes counter will be refreshed
			await driverHelper.wait(timeToRefresh);
			const changes: number = await scmProvider.getChangeCount();
			Logger.debug(`scmProvider.getChangeCount: number of changes is "${changes}"`);
			expect(changes).eql(0);
		});

		test('Push the changes', async function (): Promise<void> {
			await driverHelper.waitVisibility(webCheCodeLocators.Notification.action);
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.more);
			Logger.debug('scmProvider.openMoreActions');
			scmContextMenu = await scmProvider.openMoreActions();
			await driverHelper.waitVisibility(webCheCodeLocators.ContextMenu.itemConstructor(pushItemLabel));
			Logger.debug(`scmContextMenu.select: "${pushItemLabel}"`);
			await scmContextMenu.select(pushItemLabel);
		});

		test('Check if the changes were pushed', async function (): Promise<void> {
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.more);
			await driverHelper.wait(timeToRefresh);
			Logger.debug(`wait and click on: "${refreshButtonLabel}"`);
			await driverHelper.waitAndClick(webCheCodeLocators.ScmView.actionConstructor(refreshButtonLabel));
			const isCommitButtonDisabled: string = await driverHelper.waitAndGetElementAttribute(
				webCheCodeLocators.Notification.action,
				'aria-disabled'
			);
			expect(isCommitButtonDisabled).to.equal('true');
		});

		suiteTeardown('Open dashboard and close all other tabs', async function (): Promise<void> {
			await dashboard.openDashboard();
			await browserTabsUtil.closeAllTabsExceptCurrent();
		});

		if (BASE_TEST_CONSTANTS.DELETE_WORKSPACE_ON_SUCCESSFUL_TEST) {
			suiteTeardown('Stop and delete the workspace by API', async function (): Promise<void> {
				// to avoid a possible creating workspace which is not appeared on Dashboard yet. TODO: implement a better solution.
				await driverHelper.wait(30000);
				await testWorkspaceUtil.stopAndDeleteWorkspaceByName(WorkspaceHandlingTests.getWorkspaceName());
			});
	
			suiteTeardown('Unregister running workspace', function (): void {
				registerRunningWorkspace('');
			});
		} else {
			suiteTeardown('Registering the running workspace', function (): void {
				registerRunningWorkspace(WorkspaceHandlingTests.getWorkspaceName());
			});
		}
	}
);
