/** *******************************************************************
 * copyright (c) 2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { echo, exec, ShellString } from 'shelljs';
import { Logger } from './Logger';
import { ShellExecutor } from './ShellExecutor';
import * as path from 'path';
import { API_TEST_CONSTANTS, KubernetesCommandLineTool } from '../constants/API_TEST_CONSTANTS';
import { BASE_TEST_CONSTANTS } from '../constants/BASE_TEST_CONSTANTS';
import { OAUTH_CONSTANTS } from '../constants/OAUTH_CONSTANTS';
import { IKubernetesCommandLineToolsExecutor } from './IKubernetesCommandLineToolsExecutor';
import { inject, injectable } from 'inversify';
import { CLASSES } from '../configs/inversify.types';
import * as fs from 'fs';

@injectable()
export class KubernetesCommandLineToolsExecutor implements IKubernetesCommandLineToolsExecutor {
	private static container: string;
	private static pod: string;
	private readonly kubernetesCommandLineTool: string;
	protected _namespace: string | undefined;
	protected _workspaceName: string | undefined;

	constructor(
		@inject(CLASSES.ShellExecutor)
		protected readonly shellExecutor: ShellExecutor
	) {
		this.kubernetesCommandLineTool = API_TEST_CONSTANTS.TS_API_TEST_KUBERNETES_COMMAND_LINE_TOOL;
	}

	set namespace(value: string | undefined) {
		this._namespace = value;
	}

	set workspaceName(value: string | undefined) {
		this._workspaceName = value;
	}

	get workspaceName(): string | undefined {
		return this._workspaceName;
	}

	get namespace(): string | undefined {
		if (BASE_TEST_CONSTANTS.TEST_NAMESPACE.length > 0) {
			this._namespace = BASE_TEST_CONSTANTS.TEST_NAMESPACE;
		} else if (!this._namespace) {
			const applicationName: string = BASE_TEST_CONSTANTS.TESTING_APPLICATION_NAME();
			if (applicationName === 'default') {
				this._namespace = applicationName;
			} else {
				this._namespace = OAUTH_CONSTANTS.TS_SELENIUM_OCP_USERNAME + '-' + applicationName;
			}
		}
		return this._namespace;
	}

	// login to Openshift cluster with username and password
	loginToOcp(user: string = OAUTH_CONSTANTS.TS_SELENIUM_OCP_USERNAME, password: string = OAUTH_CONSTANTS.TS_SELENIUM_OCP_PASSWORD): void {
		if (this.kubernetesCommandLineTool === KubernetesCommandLineTool.OC) {
			Logger.debug(`${this.kubernetesCommandLineTool} - login to the "OC" client.`);
			const url: string = this.getServerUrl();
			if (this.isUserLoggedIn(user)) {
				Logger.debug(`${this.kubernetesCommandLineTool} - user already logged`);
			} else {
				Logger.debug(`${this.kubernetesCommandLineTool} - login ${url}, ${user}`);
				exec(`oc login --server=${url} -u=${user} -p=${password} --insecure-skip-tls-verify`);
			}
		} else {
			Logger.debug(`${this.kubernetesCommandLineTool} - doesn't support login command`);
		}
	}

	getContainerName(): string {
		Logger.debug(`${this.kubernetesCommandLineTool} - get container name.`);

		const output: ShellString = this.shellExecutor.executeCommand(
			`${this.kubernetesCommandLineTool} get ${KubernetesCommandLineToolsExecutor.pod} -o jsonpath='{.spec.containers[*].name}' -n ${this.namespace}`
		);
		echo('\n');
		return output.stderr ? output.stderr : output.stdout;
	}

	getWorkspacePodName(): string {
		Logger.debug(`${this.kubernetesCommandLineTool} - get workspace pod name.`);

		const output: ShellString = this.shellExecutor.executeCommand(
			`${this.kubernetesCommandLineTool} get pod -l controller.devfile.io/devworkspace_name=${this.workspaceName} -n ${this.namespace} -o name`
		);
		return output.stderr ? output.stderr : output.stdout.replace('\n', '');
	}

	deleteDevWorkspace(devfileName?: string): void {
		Logger.debug(`${this.kubernetesCommandLineTool} - delete '${this.workspaceName}' devWorkspace`);

		this.shellExecutor.executeCommand(
			`${this.kubernetesCommandLineTool} patch dw ${this.workspaceName} -n ${this.namespace} -p '{ "metadata": { "finalizers": null }}' --type merge || true`
		);
		this.shellExecutor.executeCommand(`${this.kubernetesCommandLineTool} delete dw ${this.workspaceName} -n ${this.namespace} || true`);

		let dwtName: string;
		if (devfileName === undefined) {
			dwtName = `${BASE_TEST_CONSTANTS.TS_SELENIUM_EDITOR}-${this.workspaceName}`;
		} else {
			dwtName = `${BASE_TEST_CONSTANTS.TS_SELENIUM_EDITOR}-${devfileName}`;
		}

		Logger.debug(`${this.kubernetesCommandLineTool} - delete '${dwtName}' devWorkspaceTemplate`);

		this.shellExecutor.executeCommand(`${this.kubernetesCommandLineTool} delete dwt ${dwtName} -n ${this.namespace} || true`);
	}

	applyAndWaitDevWorkspace(yamlConfiguration: string): ShellString {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		if (this.kubernetesCommandLineTool === KubernetesCommandLineTool.KUBECTL) {
			this.createNamespace();
		}
		this.applyYamlConfigurationAsStringOutput(yamlConfiguration);
		return this.waitDevWorkspace();
	}

	execInContainerCommand(commandToExecute: string, container: string = KubernetesCommandLineToolsExecutor.container): ShellString {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		return this.shellExecutor.executeCommand(
			`${this.kubernetesCommandLineTool} exec -i -t ${KubernetesCommandLineToolsExecutor.pod} -n ${this.namespace} -c ${container} -- sh -c '${commandToExecute}'`
		);
	}

	execInContainerCommandWithTimeout(
		commandToExecute: string,
		container: string = KubernetesCommandLineToolsExecutor.container,
		timeout: string = '20'
	): ShellString {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		return this.shellExecutor.executeCommand(
			` timeout ${timeout} ${this.kubernetesCommandLineTool} exec -i ${KubernetesCommandLineToolsExecutor.pod} -n ${this.namespace} -c ${container} -- sh -c '${commandToExecute}'`
		);
	}

	applyYamlConfigurationAsStringOutput(yamlConfiguration: string): ShellString {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		// write yaml configuration to the devfile file
		const randomSuffix: number = Math.floor(Math.random() * 10000);
		const devfileYamlPath: string = `/tmp/devfile-${randomSuffix}.yaml`;
		fs.writeFileSync(devfileYamlPath, yamlConfiguration);

		return this.shellExecutor.executeCommand(
			`${this.kubernetesCommandLineTool} apply -n ${this.namespace} -f ${devfileYamlPath} && rm -f ${devfileYamlPath}`
		);
	}

	applyWithoutNamespace(yamlConfiguration: string): ShellString {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		return this.shellExecutor.executeCommand(
			`cat <<EOF | ${this.kubernetesCommandLineTool} apply -f - \n` + yamlConfiguration + '\n' + 'EOF'
		);
	}

	applyYamlConfigurationAsFile(pathToFile: string): void {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		this.shellExecutor.executeCommand(`${this.kubernetesCommandLineTool} apply -n ${this.namespace} -f "${path.resolve(pathToFile)}"`);
	}

	getDevWorkspaceYamlConfiguration(): ShellString {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		return this.shellExecutor.executeCommand(
			`${this.kubernetesCommandLineTool} get dw ${this.workspaceName} -n ${this.namespace} -o yaml`
		);
	}

	waitDevWorkspace(timeout: number = 360): ShellString {
		Logger.debug(`${this.kubernetesCommandLineTool} - wait till workspace ready.`);

		const output: ShellString = this.shellExecutor.executeCommand(
			`${this.kubernetesCommandLineTool} wait -n ${this.namespace} --for=condition=Ready dw ${this.workspaceName} --timeout=${timeout}s`
		);
		this.getPodAndContainerNames();
		return output;
	}

	createNamespace(): void {
		Logger.debug(`${this.kubernetesCommandLineTool} - create namespace "${this.namespace}".`);

		this.shellExecutor.executeCommand(`${this.kubernetesCommandLineTool} create namespace ${this.namespace}`);
	}

	createProject(projectName: string): void {
		Logger.debug(`${this.kubernetesCommandLineTool} - create new project "${projectName}".`);

		this.shellExecutor.executeCommand(`${this.kubernetesCommandLineTool} new-project ${projectName} -n ${this.namespace}`);
	}

	deleteProject(projectName: string): void {
		Logger.debug(`${this.kubernetesCommandLineTool} - delete "${projectName}".`);

		this.shellExecutor.executeCommand(`${this.kubernetesCommandLineTool} delete project ${projectName} -n ${this.namespace}`);
	}

	getPodAndContainerNames(): void {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		KubernetesCommandLineToolsExecutor.pod = this.getWorkspacePodName();
		KubernetesCommandLineToolsExecutor.container = this.getContainerName();
	}

	/**
	 * @param userName
	 */
	isUserLoggedIn(userName: string): boolean {
		Logger.debug(`${this.kubernetesCommandLineTool}`);

		const whoamiCommandOutput: ShellString = this.shellExecutor.executeCommand('oc whoami && oc whoami --show-server=true');

		return whoamiCommandOutput.stdout.includes(userName) && whoamiCommandOutput.stdout.includes(this.getServerUrl());
	}

	getServerUrl(): string {
		Logger.debug(`${this.kubernetesCommandLineTool} - get server api url.`);
		const baseUrl: URL = new URL(BASE_TEST_CONSTANTS.TS_SELENIUM_BASE_URL);
		let apiHostname: string;

		if (baseUrl.hostname.includes('devspaces.apps')) {
			apiHostname = baseUrl.hostname.replace('devspaces.apps', 'api');
		} else if (baseUrl.hostname.includes('console-openshift-console.apps')) {
			apiHostname = baseUrl.hostname.replace('console-openshift-console.apps', 'api');
		} else {
			throw new Error(`Unexpected base URL hostname format: ${baseUrl.hostname}`);
		}

		const apiUrl: string = `https://${apiHostname}:6443`;
		Logger.debug(`Generated API server URL: ${apiUrl}`);
		return apiUrl;
	}
}

@injectable()
export class ContainerTerminal extends KubernetesCommandLineToolsExecutor {
	constructor(
		@inject(CLASSES.ShellExecutor)
		readonly shellExecutor: ShellExecutor,
		@inject(CLASSES.KubernetesCommandLineToolsExecutor)
		readonly cluster: KubernetesCommandLineToolsExecutor
	) {
		super(shellExecutor);
	}

	ls(path: string = ''): ShellString {
		return this.execInContainerCommand('ls ' + path);
	}

	pwd(): ShellString {
		return this.execInContainerCommand('pwd');
	}

	cd(path: string): ShellString {
		return this.execInContainerCommand('cd ' + path);
	}

	gitClone(repository: string): ShellString {
		return this.execInContainerCommand('git clone ' + repository);
	}

	removeFolder(path: string): ShellString {
		return this.execInContainerCommand('rm -rf ' + path);
	}
}
