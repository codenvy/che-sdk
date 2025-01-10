/** *******************************************************************
 * copyright (c) 2024 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { CLASSES } from '../../configs/inversify.types';
import { e2eContainer } from '../../configs/inversify.config';
import { LoginTests } from '../../tests-library/LoginTests';
import { OcpMainPage } from '../../pageobjects/openshift/OcpMainPage';
import { BASE_TEST_CONSTANTS } from '../../constants/BASE_TEST_CONSTANTS';
import { KubernetesCommandLineToolsExecutor } from '../../utils/KubernetesCommandLineToolsExecutor';
import { ShellExecutor } from '../../utils/ShellExecutor';
import { DriverHelper } from '../../utils/DriverHelper';
import { WebTerminalPage } from '../../pageobjects/webterminal/WebTerminalPage';
import { expect } from 'chai';
import YAML from 'yaml';

suite(`Login to Openshift console and start WebTerminal ${BASE_TEST_CONSTANTS.TEST_ENVIRONMENT}`, function (): void {
	const loginTests: LoginTests = e2eContainer.get(CLASSES.LoginTests);
	const ocpMainPage: OcpMainPage = e2eContainer.get(CLASSES.OcpMainPage);
	const webTerminal: WebTerminalPage = e2eContainer.get(CLASSES.WebTerminalPage);
	const shellExecutor: ShellExecutor = e2eContainer.get(CLASSES.ShellExecutor);
	const kubernetesCommandLineToolsExecutor: KubernetesCommandLineToolsExecutor = e2eContainer.get(
		CLASSES.KubernetesCommandLineToolsExecutor
	);
	const defaultWTOProjectNameForAdmin: string = 'openshift-terminal';
	const driverHelper: DriverHelper = e2eContainer.get(CLASSES.DriverHelper);
	const webTerminalToolContainerName: string = 'web-terminal-tooling';
	const fileForVerificationTerminalCommands: string = 'result.txt';

	suiteSetup(function (): void {
		kubernetesCommandLineToolsExecutor.loginToOcp('kubeadmin');
	});

	suiteTeardown(function (): void {
		shellExecutor.executeArbitraryShellScript(`oc delete dw --all -n ${defaultWTOProjectNameForAdmin}`);
	});
	loginTests.loginIntoOcpConsole();
	test('Open Web Terminal after first installation', async function (): Promise<void> {
		await ocpMainPage.waitOpenMainPage();
		await driverHelper.refreshPage();
		await webTerminal.clickOnWebTerminalIcon();
	});
	test('Verify first started WTO widget and disabled state Project field under admin user', async function (): Promise<void> {
		await webTerminal.waitTerminalWidget();
		expect(await webTerminal.waitDisabledProjectFieldAndGetProjectName()).equal(defaultWTOProjectNameForAdmin);
	});
	test('Check starting Web Terminal under admin', async function (): Promise<void> {
		kubernetesCommandLineToolsExecutor.namespace = await webTerminal.getAdminProjectName();
		await webTerminal.clickOnStartWebTerminalButton();
		await webTerminal.waitTerminalIsStarted();
		await webTerminal.typeAndEnterIntoWebTerminal(`oc whoami > ${fileForVerificationTerminalCommands}`);
		const devWorkspaceYaml: string = shellExecutor.executeArbitraryShellScript(
			`oc get dw -n ${kubernetesCommandLineToolsExecutor.namespace} -o yaml`
		);
		kubernetesCommandLineToolsExecutor.workspaceName = YAML.parse(devWorkspaceYaml).items[0].metadata.name;
		kubernetesCommandLineToolsExecutor.getPodAndContainerNames();
		const commandResult: string = kubernetesCommandLineToolsExecutor.execInContainerCommand(
			`cat /home/user/${fileForVerificationTerminalCommands}`,
			webTerminalToolContainerName
		);
		expect(commandResult).contains('kube:admin');
	});
	test('Verify help command under admin user', async function (): Promise<void> {
		const expectedStrings: string[] = [
			'OpenShift CLI',
			'Kubernetes CLI',
			'Kustomize CLI (built-in to kubectl)',
			'Helm CLI',
			'KNative CLI',
			'Tekton CLI',
			'Submariner CLI',
			'KubeVirt CLI',
			'jq'
		];

		await webTerminal.typeAndEnterIntoWebTerminal(`help > ${fileForVerificationTerminalCommands}`);

		// need 5 sec. delay for finishing writing results of help command into txt file. The 5 sec delay is enough for the most cases
		await driverHelper.wait(5000);
		const commandResult: string = kubernetesCommandLineToolsExecutor.execInContainerCommand(
			`cat /home/user/${fileForVerificationTerminalCommands}`,
			webTerminalToolContainerName
		);

		expectedStrings.forEach((expectedString): void => {
			expect(commandResult).to.contain(expectedString);
		});
	});

	test('Verify help command under admin user', async function (): Promise<void> {
		await webTerminal.typeAndEnterIntoWebTerminal('wtoctl set timeout 30s');
		await webTerminal.waitTerminalInactivity();
	});
});
