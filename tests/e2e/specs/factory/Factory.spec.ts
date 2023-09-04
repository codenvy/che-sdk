/** *******************************************************************
 * copyright (c) 2021 Red Hat, Inc.
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
	ModalDialog,
	NewScmView,
	SideBarView,
	SingleScmProvider,
	TextEditor,
	ViewControl,
	ViewItem,
	ViewSection
} from 'monaco-page-objects';
import { expect } from 'chai';
import { OauthPage } from '../../pageobjects/git-providers/OauthPage';
import { StringUtil } from '../../utils/StringUtil';
import { registerRunningWorkspace } from '../MochaHooks';
import { BrowserTabsUtil } from '../../utils/BrowserTabsUtil';
import { WorkspaceHandlingTests } from '../../tests-library/WorkspaceHandlingTests';
import { CLASSES } from '../../configs/inversify.types';
import { DriverHelper } from '../../utils/DriverHelper';
import { ProjectAndFileTests } from '../../tests-library/ProjectAndFileTests';
import { Logger } from '../../utils/Logger';
import { LoginTests } from '../../tests-library/LoginTests';
import { OAUTH_CONSTANTS } from '../../constants/OAUTH_CONSTANTS';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';
import { FACTORY_TEST_CONSTANTS } from '../../constants/FACTORY_TEST_CONSTANTS';

suite(
	`Create a workspace via launching a factory from the ${FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_GIT_PROVIDER} repository`,
	function (): void {
		const browserTabsUtil: BrowserTabsUtil = e2eContainer.get(CLASSES.BrowserTabsUtil);
		const workspaceHandlingTests: WorkspaceHandlingTests = e2eContainer.get(CLASSES.WorkspaceHandlingTests);
		const projectAndFileTests: ProjectAndFileTests = e2eContainer.get(CLASSES.ProjectAndFileTests);
		const driverHelper: DriverHelper = e2eContainer.get(CLASSES.DriverHelper);
		const loginTests: LoginTests = e2eContainer.get(CLASSES.LoginTests);
		const webCheCodeLocators: Locators = e2eContainer.get(CLASSES.CheCodeLocatorLoader);
		const oauthPage: OauthPage = e2eContainer.get(CLASSES.OauthPage);

		let projectSection: ViewSection;
		let scmProvider: SingleScmProvider;
		let rest: SingleScmProvider[];
		let scmContextMenu: ContextMenu;

		// test specific data
		const timeToRefresh: number = 1500;
		const changesToCommit: string = new Date().getTime().toString();
		const fileToChange: string = 'Date.txt';
		const commitChangesButtonLabel: string = `Commit Changes on "${FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_GIT_REPO_BRANCH}"`;
		const refreshButtonLabel: string = 'Refresh';
		const pushItemLabel: string = 'Push';
		let testRepoProjectName: string;

		loginTests.loginIntoChe();
		test('Navigate to the factory URL', async function (): Promise<void> {
			await browserTabsUtil.navigateTo(FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_URL());
		});

		if (OAUTH_CONSTANTS.TS_SELENIUM_GIT_PROVIDER_OAUTH) {
			test(`Authorize with a ${FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_GIT_PROVIDER} OAuth`, async function (): Promise<void> {
				await oauthPage.login();
				await oauthPage.waitOauthPage();
				await oauthPage.confirmAccess();
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
			projectSection = await new SideBarView().getContent().getSection(testRepoProjectName);
		});

		test('Check if the project files were imported', async function (): Promise<void> {
			const label: string = BASE_TEST_CONSTANTS.TS_SELENIUM_PROJECT_ROOT_FILE_NAME;
			Logger.debug(`projectSection.findItem: find ${label}`);
			const isFileImported: ViewItem | undefined = await projectSection.findItem(label);
			expect(isFileImported).not.eqls(undefined);
		});

		test('Accept the project as a trusted one', async function (): Promise<void> {
			const buttonYesITrustTheAuthors: string = 'Yes, I trust the authors';
			const trustedProjectDialog: ModalDialog = new ModalDialog();
			await driverHelper.waitVisibility(webCheCodeLocators.WelcomeContent.button);
			Logger.debug(`trustedProjectDialog.pushButton: "${buttonYesITrustTheAuthors}"`);
			await trustedProjectDialog.pushButton(buttonYesITrustTheAuthors);
		});

		test('Make changes to the file', async function (): Promise<void> {
			Logger.debug(`projectSection.openItem: "${fileToChange}"`);
			await projectSection.openItem(fileToChange);
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
			Logger.debug(`scmView.getProviders: "${JSON.stringify(scmProvider)}, ${rest}"`);
		});

		test('Check if the changes are displayed in the source control manager', async function (): Promise<void> {
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.more);
			await driverHelper.wait(timeToRefresh);
			Logger.debug(`scmProvider.takeAction: "${refreshButtonLabel}"`);
			await scmProvider.takeAction(refreshButtonLabel);
			// wait while changes counter will be refreshed
			await driverHelper.wait(timeToRefresh);
			const changes: number = await scmProvider.getChangeCount();
			Logger.debug(`scmProvider.getChangeCount: number of changes is "${changes}"`);
			expect(changes).eql(1);
		});

		test('Stage the changes', async function (): Promise<void> {
			await driverHelper.waitVisibility(webCheCodeLocators.ScmView.more);
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
			Logger.debug(`scmProvider.takeAction: "${refreshButtonLabel}"`);
			await scmProvider.takeAction(refreshButtonLabel);
			// wait while changes counter will be refreshed
			await driverHelper.wait(timeToRefresh);
			const changes: number = await scmProvider.getChangeCount();
			Logger.debug(`scmProvider.getChangeCount: number of changes is "${changes}"`);
			expect(changes).eql(0);
		});

		test('Push the changes', async function (): Promise<void> {
			await driverHelper.waitVisibility(
				webCheCodeLocators.ScmView.actionConstructor(
					`Push 1 commits to origin/${FACTORY_TEST_CONSTANTS.TS_SELENIUM_FACTORY_GIT_REPO_BRANCH}`
				)
			);
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
			Logger.debug(`scmProvider.takeAction: "${refreshButtonLabel}"`);
			await scmProvider.takeAction(refreshButtonLabel);
			const isCommitButtonDisabled: string = await driverHelper.waitAndGetElementAttribute(
				webCheCodeLocators.ScmView.actionConstructor(commitChangesButtonLabel),
				'aria-disabled'
			);
			expect(isCommitButtonDisabled).eql('true');
		});

		test('Stop the workspace', async function (): Promise<void> {
			await workspaceHandlingTests.stopWorkspace(WorkspaceHandlingTests.getWorkspaceName());
			await browserTabsUtil.closeAllTabsExceptCurrent();
		});

		test('Delete the workspace', async function (): Promise<void> {
			await workspaceHandlingTests.removeWorkspace(WorkspaceHandlingTests.getWorkspaceName());
		});

		loginTests.logoutFromChe();
	}
);
