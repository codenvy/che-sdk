/** *******************************************************************
 * copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import { SideBarView, ViewSection } from 'monaco-page-objects';
import { registerRunningWorkspace } from '../MochaHooks';
import { LoginTests } from '../../tests-library/LoginTests';
import { e2eContainer } from '../../configs/inversify.config';
import { CLASSES } from '../../configs/inversify.types';
import { WorkspaceHandlingTests } from '../../tests-library/WorkspaceHandlingTests';
import { ProjectAndFileTests } from '../../tests-library/ProjectAndFileTests';
import { Logger } from '../../utils/Logger';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';
import { BrowserTabsUtil } from '../../utils/BrowserTabsUtil';

const stackName: string = 'Java 11 with Quarkus';

suite(`The ${stackName} userstory`, function (): void {
	const projectAndFileTests: ProjectAndFileTests = e2eContainer.get(CLASSES.ProjectAndFileTests);
	const workspaceHandlingTests: WorkspaceHandlingTests = e2eContainer.get(CLASSES.WorkspaceHandlingTests);
	const loginTests: LoginTests = e2eContainer.get(CLASSES.LoginTests);
	const browserTabsUtil: BrowserTabsUtil = e2eContainer.get(CLASSES.BrowserTabsUtil);

	let projectSection: ViewSection;

	const projectName: string = 'quarkus-quickstarts';

	loginTests.loginIntoChe();

	test(`Create and open new workspace, stack:${stackName}`, async function (): Promise<void> {
		await workspaceHandlingTests.createAndOpenWorkspace(stackName);
	});

	test('Obtain workspace name from workspace loader page', async function (): Promise<void> {
		await workspaceHandlingTests.obtainWorkspaceNameFromStartingPage();
	});

	test('Register running workspace', function (): void {
		registerRunningWorkspace(WorkspaceHandlingTests.getWorkspaceName());
	});

	test('Wait workspace readiness', async function (): Promise<void> {
		await projectAndFileTests.waitWorkspaceReadinessForCheCodeEditor();
	});

	test('Check a project folder has been created', async function (): Promise<void> {
		projectSection = await new SideBarView().getContent().getSection(projectName);
		Logger.debug(`new SideBarView().getContent().getSection: get ${projectName}`);
	});

	test('Check the project files was imported', async function (): Promise<void> {
		await projectSection.findItem(BASE_TEST_CONSTANTS.TS_SELENIUM_PROJECT_ROOT_FILE_NAME);
		Logger.debug(`projectSection.findItem: find ${BASE_TEST_CONSTANTS.TS_SELENIUM_PROJECT_ROOT_FILE_NAME}`);
	});

	test('Stop the workspace', async function (): Promise<void> {
		await workspaceHandlingTests.stopWorkspace(WorkspaceHandlingTests.getWorkspaceName());
		await browserTabsUtil.closeAllTabsExceptCurrent();
	});

	test('Delete the workspace', async function (): Promise<void> {
		await workspaceHandlingTests.removeWorkspace(WorkspaceHandlingTests.getWorkspaceName());
	});
	loginTests.logoutFromChe();
});
