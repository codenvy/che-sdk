/** *******************************************************************
 * copyright (c) 2019-2024 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { inject, injectable } from 'inversify';
import 'reflect-metadata';
import { CLASSES } from '../../configs/inversify.types';
import { By } from 'selenium-webdriver';
import { DriverHelper } from '../../utils/DriverHelper';
import { TIMEOUT_CONSTANTS } from '../../constants/TIMEOUT_CONSTANTS';
import { Workspaces } from './Workspaces';
import { Logger } from '../../utils/Logger';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';

@injectable()
export class Dashboard {
	private static readonly WORKSPACES_BUTTON: By = By.xpath('//div[@id="page-sidebar"]//a[contains(text(), "Workspaces (")]');
	private static readonly CREATE_WORKSPACE_BUTTON: By = By.xpath('//div[@id="page-sidebar"]//a[text()="Create Workspace"]');
	private static readonly LOADER_PAGE_STEP_TITLES: By = By.xpath('//*[@data-testid="step-title"]');
	private static readonly STARTING_PAGE_LOADER: By = By.css('.main-page-loader');
	private static readonly LOADER_ALERT: By = By.xpath('//*[@data-testid="loader-alert"]');
	private static readonly LOGOUT_BUTTON: By = By.xpath('//button[text()="Logout"]');
	private static readonly USER_SETTINGS_DROPDOWN: By = By.xpath('//header//button/span[text()!=""]//parent::button');
	private static readonly INFO_DROPDOWN_BUTTON: By = By.xpath('//button[@aria-label="About Menu"]');
	private static readonly ABOUT_DIALOG_WINDOW_CLOSE_BUTTON: By = By.xpath('//button[@aria-label="Close Dialog"]');
	private static readonly EXISTING_WORKSPACE_FOUND_ALERT: By = By.xpath('//h4[text()="Existing workspace found"]');
	private static readonly CREATE_NEW_WORKSPACE_LINK: By = By.xpath('//button[text()="Create a new workspace"]');
	private static readonly ABOUT_DIALOG_ITEM_DATA_TEST_IDS: any = {
		serverVersion: 'server-version',
		dashboardVersion: 'dashboard-version',
		browserName: 'browser-name',
		browserOs: 'browser-os',
		browserVersion: 'browser-version',
		username: 'username'
	};
	private static readonly CONTINUE_WITH_DEFAULT_DEVFILE_BUTTON: By = By.xpath('//button[text()="Continue with default devfile"]');

	constructor(
		@inject(CLASSES.DriverHelper)
		readonly driverHelper: DriverHelper,
		@inject(CLASSES.Workspaces) private readonly workspaces: Workspaces
	) {}

	async stopWorkspaceByUI(workspaceName: string): Promise<void> {
		Logger.debug(`"${workspaceName}"`);

		await this.clickWorkspacesButton();
		await this.workspaces.waitPage();
		await this.workspaces.waitWorkspaceListItem(workspaceName);
		await this.workspaces.waitWorkspaceWithRunningStatus(workspaceName);
		await this.workspaces.stopWorkspaceByActionsButton(workspaceName);
		await this.workspaces.waitWorkspaceWithStoppedStatus(workspaceName);
	}

	async deleteStoppedWorkspaceByUI(workspaceName: string): Promise<void> {
		Logger.debug(`"${workspaceName}"`);

		await this.clickWorkspacesButton();
		await this.workspaces.waitPage();
		await this.workspaces.waitWorkspaceListItem(workspaceName);
		await this.workspaces.deleteWorkspaceByActionsButton(workspaceName);
		await this.workspaces.waitPage();
		await this.workspaces.waitWorkspaceListItemAbsence(workspaceName);
	}

	async stopAndRemoveWorkspaceByUI(workspaceName: string): Promise<void> {
		Logger.debug(`"${workspaceName}"`);

		await this.stopWorkspaceByUI(workspaceName);
		await this.deleteStoppedWorkspaceByUI(workspaceName);
	}

	async openDashboard(): Promise<void> {
		Logger.debug();

		await this.driverHelper.navigateToUrl(BASE_TEST_CONSTANTS.TS_SELENIUM_BASE_URL);
		await this.waitPage();
	}

	async openAboutMenu(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(Dashboard.INFO_DROPDOWN_BUTTON);
	}

	async selectAboutMenuItem(text: string): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(this.getAboutMenuItemButtonLocator(text));
	}

	async waitAboutDialogWindowMenuElements(timeout: number = TIMEOUT_CONSTANTS.TS_COMMON_DASHBOARD_WAIT_TIMEOUT): Promise<void> {
		Logger.debug();

		for (const testId of Object.values(Dashboard.ABOUT_DIALOG_ITEM_DATA_TEST_IDS)) {
			const workspaceDetailsTabLocator: By = this.getAboutDialogWindowItemLocator(<string>testId);

			await this.driverHelper.waitVisibility(workspaceDetailsTabLocator, timeout);
		}
	}

	async getApplicationVersionFromAboutDialogWindow(): Promise<string> {
		Logger.debug();

		return await this.driverHelper.waitAndGetText(
			this.getAboutDialogWindowItemLocator(Dashboard.ABOUT_DIALOG_ITEM_DATA_TEST_IDS.serverVersion)
		);
	}

	async getUsernameFromAboutDialogWindow(): Promise<string> {
		Logger.debug();

		return await this.driverHelper.waitAndGetText(
			this.getAboutDialogWindowItemLocator(Dashboard.ABOUT_DIALOG_ITEM_DATA_TEST_IDS.username)
		);
	}

	async waitPage(timeout: number = TIMEOUT_CONSTANTS.TS_SELENIUM_LOAD_PAGE_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitVisibility(Dashboard.WORKSPACES_BUTTON, timeout);
		await this.driverHelper.waitVisibility(Dashboard.CREATE_WORKSPACE_BUTTON, timeout);
	}

	async clickWorkspacesButton(timeout: number = TIMEOUT_CONSTANTS.TS_CLICK_DASHBOARD_ITEM_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(Dashboard.WORKSPACES_BUTTON, timeout);
	}

	async clickCreateWorkspaceButton(timeout: number = TIMEOUT_CONSTANTS.TS_CLICK_DASHBOARD_ITEM_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(Dashboard.CREATE_WORKSPACE_BUTTON, timeout);
	}

	async getLoaderAlert(timeout: number = TIMEOUT_CONSTANTS.TS_WAIT_LOADER_PRESENCE_TIMEOUT): Promise<string> {
		Logger.debug();

		return await this.driverHelper.waitAndGetText(Dashboard.LOADER_ALERT, timeout);
	}

	async waitLoader(timeout: number = TIMEOUT_CONSTANTS.TS_WAIT_LOADER_PRESENCE_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAllPresence(Dashboard.LOADER_PAGE_STEP_TITLES, timeout);
	}

	async waitStartingPageLoaderDisappearance(timeout: number = TIMEOUT_CONSTANTS.TS_COMMON_DASHBOARD_WAIT_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitDisappearance(Dashboard.STARTING_PAGE_LOADER, timeout);
		await this.driverHelper.wait(TIMEOUT_CONSTANTS.TS_SELENIUM_DEFAULT_POLLING);
	}

	async closeAboutDialogWindow(): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(Dashboard.ABOUT_DIALOG_WINDOW_CLOSE_BUTTON);
	}

	async waitExistingWorkspaceFoundAlert(timeout: number = TIMEOUT_CONSTANTS.TS_WAIT_LOADER_PRESENCE_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitVisibility(Dashboard.EXISTING_WORKSPACE_FOUND_ALERT, timeout);
	}

	async clickOnCreateNewWorkspaceButton(timeout: number = TIMEOUT_CONSTANTS.TS_CLICK_DASHBOARD_ITEM_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(Dashboard.CREATE_NEW_WORKSPACE_LINK, timeout);
	}

	async logout(timeout: number = TIMEOUT_CONSTANTS.TS_COMMON_DASHBOARD_WAIT_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.openDashboard();
		await this.driverHelper.waitAndClick(Dashboard.USER_SETTINGS_DROPDOWN, timeout);
		await this.driverHelper.waitAndClick(Dashboard.LOGOUT_BUTTON, timeout);
		await this.driverHelper.waitDisappearance(Dashboard.USER_SETTINGS_DROPDOWN, timeout);
	}

	async clickContinueWithDefaultDevfileButton(timeout: number = TIMEOUT_CONSTANTS.TS_COMMON_DASHBOARD_WAIT_TIMEOUT): Promise<void> {
		Logger.debug();

		await this.driverHelper.waitAndClick(Dashboard.CONTINUE_WITH_DEFAULT_DEVFILE_BUTTON, timeout);
	}

	private getAboutMenuItemButtonLocator(text: string): By {
		return By.xpath(`//li/button[text()="${text}"]`);
	}

	private getAboutDialogWindowItemLocator(itemDataTestId: string): By {
		return By.xpath(`//dd[@data-testid="${itemDataTestId}"]`);
	}
}
