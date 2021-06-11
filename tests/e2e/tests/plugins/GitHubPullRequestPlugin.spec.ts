/*********************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import 'reflect-metadata';
import { e2eContainer } from '../../inversify.config';
import { CLASSES, TYPES } from '../../inversify.types';
import { Ide } from '../../pageobjects/ide/Ide';
import { TimeoutConstants } from '../../TimeoutConstants';
import { TestConstants } from '../../TestConstants';
import { ProjectTree } from '../../pageobjects/ide/ProjectTree';
import { WorkspaceHandlingTests } from '../../testsLibrary/WorkspaceHandlingTests';
import { Logger } from '../../utils/Logger';
import { DriverHelper } from '../../utils/DriverHelper';
import { GitHubPullRequestPlugin } from '../../pageobjects/ide/plugins/GitHubPullRequestPlugin';
import { GitLoginPage } from '../../pageobjects/third-parties/GitLoginPage';
import { GitOauthAppsSettings } from '../../pageobjects/third-parties/GitOauthAppsSettings';
import { WorkspaceNameHandler } from '../../utils/WorkspaceNameHandler';
import { KeycloackUrlHandler } from '../../utils/KeycloackUrlHandler';
import { GitPlugin } from '../../pageobjects/ide/GitPlugin';
import { TopMenu } from '../../pageobjects/ide/TopMenu';
import { QuickOpenContainer } from '../../pageobjects/ide/QuickOpenContainer';
import { Editor } from '../../pageobjects/ide/Editor';

const ide: Ide = e2eContainer.get(CLASSES.Ide);
const projectTree: ProjectTree = e2eContainer.get(CLASSES.ProjectTree);
const workspaceHandling: WorkspaceHandlingTests = e2eContainer.get(CLASSES.WorkspaceHandlingTests);
const driverHelper: DriverHelper = e2eContainer.get(CLASSES.DriverHelper);
const gitHubPullRequestPlugin: GitHubPullRequestPlugin = e2eContainer.get(CLASSES.GitHubPullRequestPlugin);
const githubLoginPage: GitLoginPage = e2eContainer.get(CLASSES.GitLoginPage);
const gitOauthAppsSettings: GitOauthAppsSettings = e2eContainer.get(CLASSES.GitOauthAppsSettings);
const gitPlugin: GitPlugin = e2eContainer.get(CLASSES.GitPlugin);
const topMenu: TopMenu = e2eContainer.get(CLASSES.TopMenu);
const quickOpenContainer: QuickOpenContainer = e2eContainer.get(CLASSES.QuickOpenContainer);
const editor: Editor = e2eContainer.get(CLASSES.Editor);

const devfileUrl: string = `https://raw.githubusercontent.com/eclipse/che/main/tests/e2e/files/devfiles/plugins/GitHubPullRequestPlugin.yaml`;
const factoryUrl: string = `${TestConstants.TS_SELENIUM_BASE_URL}/f?url=${devfileUrl}`;
const branchName: string =  WorkspaceNameHandler.generateWorkspaceName('ghPrPlugin-', 10);
const projectName: string = 'Spoon-Knife';
const oAuthAppName: string = 'eclipse-che';
const changedFile: string = 'README.md';
const currentDate: string = Date.now().toString();

suite(`The 'GitHubPullRequestPlugin' test`, async () => {
    let workspaceName: string = '';

    suite('Setup github', async () => {
        test('Login to github', async () => {
            await gitOauthAppsSettings.openPage();
            await githubLoginPage.login();
        });

        test('Open application settings', async () => {
            await gitOauthAppsSettings.openOauthApp(oAuthAppName);
        });

        test('Configure oauth application', async () => {
            await gitOauthAppsSettings.scrollToUpdateApplicationButton();

            await gitOauthAppsSettings.typeHomePageUrl(TestConstants.TS_SELENIUM_BASE_URL);
            await gitOauthAppsSettings.typeCallbackUrl(KeycloackUrlHandler.getIdentityCallbackUrl());
            await gitOauthAppsSettings.clickUpdateApplicationButton();
        });
    });

    suite('Check the GH PR plugin', async () => {
        test('Create workspace using factory', async () => {
            await driverHelper.navigateToUrl(factoryUrl);
        });

        test('Wait until created workspace is started', async () => {
            await ide.waitAndSwitchToIdeFrame();
            await ide.waitIde(TimeoutConstants.TS_SELENIUM_START_WORKSPACE_TIMEOUT);

            await gitHubPullRequestPlugin.waitViewIcon();
            await projectTree.openProjectTreeContainer();
            await projectTree.waitProjectImported(projectName, 'README.md');

            workspaceName = await WorkspaceNameHandler.getNameFromUrl();
        });
        
        test('Create new branch', async () => {
            await topMenu.selectOption('View', 'Find Command...');
            await quickOpenContainer.typeAndSelectSuggestion('branch', 'Git: Create Branch...');
            await quickOpenContainer.typeAndSelectSuggestion(branchName, `Please provide a new branch name (Press 'Enter' to confirm your input or 'Escape' to cancel)`);
    
            await projectTree.expandPathAndOpenFile('Spoon-Knife', changedFile);
            await editor.type(changedFile, currentDate + '\n', 1);
            await gitPlugin.openGitPluginContainer();
            await gitPlugin.waitChangedFileInChagesList(changedFile);
            await gitPlugin.stageAllChanges(changedFile);
            await gitPlugin.waitChangedFileInChagesList(changedFile);
            await gitPlugin.typeCommitMessage(`ghPrPlugin-${currentDate}`);
            await gitPlugin.commitFromCommandMenu();
        });
        
        test('Open GH PR plugin', async () => {
            await gitHubPullRequestPlugin.openView();
        });

        test('Authorize workspace to use GitHub', async () => {
            const loginNotificationText: string = `wants to sign in using GitHub.`;
            const buttonText: string = 'Allow';

            await gitHubPullRequestPlugin.clickSignInButton();
            await ide.waitNotificationAndClickOnButton(loginNotificationText, buttonText);
        });

        test('Check PR plugin connected to PR', async () => {
            await gitHubPullRequestPlugin.expandTreeItem('Created By Me');
            await gitHubPullRequestPlugin.waitTreeItem('[DO NOT MERGE] gh-pr-plugin-test (#3) by @chepullreq4');
        });

        test('Create PR', async () => {
            const permissionsNotificationText: string = `wants to sign in using GitHub.`;
            const buttonText: string = 'Allow';

            await gitHubPullRequestPlugin.createPrFromCommandMenu();
            await quickOpenContainer.clickOnContainerItem('chepullreq4:Spoon-Knife');
            await quickOpenContainer.clickOnContainerItem(`Choose target branch for chepullreq4/Spoon-Knife (Press 'Enter' to confirm your input or 'Escape' to cancel)`);
            await quickOpenContainer.clickOnContainerItem(`The branch '${branchName}' is not published yet, pick a name for the upstream branch (Press 'Enter' to confirm your input or 'Escape' to cancel)`);
            
            await ide.waitNotificationAndClickOnButton(permissionsNotificationText, buttonText);
            await quickOpenContainer.clickOnContainerItem('commit');
            await gitHubPullRequestPlugin.waitTreeItem(`ghPrPlugin-${currentDate}`)
        });
    });

    suite('Stopping and deleting the workspace', async () => {
        test(`Stop and remove workspace`, async () => {
            if (TestConstants.TS_DELETE_PLUGINS_TEST_WORKSPACE === 'true') {
                await workspaceHandling.stopAndRemoveWorkspace(workspaceName);
                return;
            }

            Logger.info(`As far as the "TS_DELETE_PLUGINS_TEST_WORKSPACE" value is "false the workspace deletion is skipped"`);
        });
    });

});
