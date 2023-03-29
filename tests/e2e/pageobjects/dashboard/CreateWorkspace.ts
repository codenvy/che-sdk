/*********************************************************************
 * Copyright (c) 2019-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import { injectable, inject } from 'inversify';
import { CLASSES } from '../../configs/inversify.types';
import { DriverHelper } from '../../utils/DriverHelper';
import { By, Key } from 'selenium-webdriver';
import { Logger } from '../../utils/Logger';
import { TimeoutConstants } from '../../constants/TimeoutConstants';
import { TestConstants } from '../../constants/TestConstants';

@injectable()
export class CreateWorkspace {
    static readonly FACTORY_URL_LOCATOR: By = By.xpath(`//input[@id="git-repo-url"]`);

    constructor(@inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper) { }

    async waitTitleContains(expectedText: string, timeout: number = TimeoutConstants.TS_COMMON_DASHBOARD_WAIT_TIMEOUT): Promise<void> {
        Logger.debug(`CreateWorkspace.waitTitleContains text: "${expectedText}"`);

        const pageTitleLocator: By = By.xpath(`//h1[contains(text(), '${expectedText}')]`);

        await this.driverHelper.waitVisibility(pageTitleLocator, timeout);
    }

    async waitPage(timeout: number = TimeoutConstants.TS_SELENIUM_LOAD_PAGE_TIMEOUT): Promise<void> {
        Logger.debug('CreateWorkspace.waitPage');

        await this.waitTitleContains('Create Workspace', timeout);
    }

    async clickOnSampleNoEditorSelection(sampleName: string, timeout: number = TimeoutConstants.TS_CLICK_DASHBOARD_ITEM_TIMEOUT): Promise<void> {
        Logger.debug(`CreateWorkspace.clickOnSample sampleName: "${sampleName}"`);

        const sampleLocator: By = this.getSampleLocator(sampleName);

        await this.driverHelper.waitAndClick(sampleLocator, timeout);
    }

    async clickOnSampleForSpecificEditor(sampleName: string, timeout: number = TimeoutConstants.TS_CLICK_DASHBOARD_ITEM_TIMEOUT): Promise<void> {
        await this.clickOnEditorsDropdownListButton(sampleName, timeout);

        Logger.debug(`CreateWorkspace.clickOnSampleForSpecificEditor sampleName: "${sampleName}"`);

        const sampleLocator: By = this.getSampleLocatorWithSpecificEditor(sampleName);
        await this.driverHelper.waitAndClick(sampleLocator, timeout);
    }

    async importFromGitUsingUI(factoryUrl: string, timeout: number = TimeoutConstants.TS_CLICK_DASHBOARD_ITEM_TIMEOUT): Promise<void> {
        Logger.debug(`CreateWorkspace.importFromGitUsingUI factoryUrl: "${factoryUrl}"`);
        await this.driverHelper.waitVisibility(CreateWorkspace.FACTORY_URL_LOCATOR, timeout);
        await this.driverHelper.type(CreateWorkspace.FACTORY_URL_LOCATOR, Key.chord(factoryUrl, Key.ENTER), timeout);
    }

    private async clickOnEditorsDropdownListButton(sampleName: string, timeout: number): Promise<void> {
        Logger.debug(`CreateWorkspace.clickOnSample sampleName: "${sampleName}, editor ${TestConstants.TS_SELENIUM_EDITOR}"`);

        const editorDropdownListLocator: By = this.getEditorsDropdownListLocator(sampleName);
        await this.driverHelper.waitAndClick(editorDropdownListLocator, timeout);
    }

    private getEditorsDropdownListLocator(sampleName: string): By {
        return By.xpath(`//div[text()=\'${sampleName}\']//parent::article//button`);
    }

    private getSampleLocatorWithSpecificEditor(sampleName: string): By {
        let editor: string = '';
        switch (process.env.TS_SELENIUM_EDITOR) {
            case 'che-code':
                editor = 'code';
                break;
            default:
                throw new Error(`Unsupported editor ${process.env.TS_SELENIUM_EDITOR}`);
        }

        Logger.trace(`CreateWorkspace.getSampleLocatorWithSpecificEditor sampleName: ${sampleName}, editor "${editor}"`);

        return By.xpath(`//div[text()='${sampleName}']//parent::article//span[text()[
                contains(
                translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'),
                '${editor}')]
            ]//parent::a`);
    }

    private getSampleLocator(sampleName: string): By {
        Logger.trace(`CreateWorkspace.getSampleLocator sampleName: ${sampleName}, used default editor`);

        return By.xpath(`//article[contains(@class, 'sample-card')]//div[text()='${sampleName}']`);
    }
}
