/** *******************************************************************
 * copyright (c) 2020-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
export enum Platform {
	OPENSHIFT = 'openshift',
	KUBERNETES = 'kubernetes'
}

export const BASE_TEST_CONSTANTS: {
	OCP_INFRA: string;
	DELETE_WORKSPACE_ON_FAILED_TEST: boolean;
	DELETE_WORKSPACE_ON_SUCCESSFUL_TEST: boolean;
	IS_RESTART_EXISTING_WORKSPACE: boolean;
	IS_CLUSTER_DISCONNECTED: () => boolean;
	IS_PRODUCT_DOCUMENTATION_RELEASED: any;
	OCP_VERSION: string;
	TESTING_APPLICATION_VERSION: string;
	TEST_ENVIRONMENT: string;
	TS_DEBUG_MODE: boolean;
	TS_LOAD_TESTS: string;
	TS_PLATFORM: string;
	TS_SELENIUM_BASE_URL: string;
	TS_SELENIUM_DASHBOARD_SAMPLE_NAME: string;
	TS_SELENIUM_EDITOR: string;
	TS_SELENIUM_HAPPY_PATH_WORKSPACE_NAME: string;
	TS_SELENIUM_PROJECT_ROOT_FILE_NAME: string;
	TS_SELENIUM_REQUEST_INTERCEPTOR: boolean;
	TS_SELENIUM_RESPONSE_INTERCEPTOR: boolean;
	TESTING_APPLICATION_NAME: () => string;
	TEST_NAMESPACE: string;
} = {
	/**
	 * base URL of the application which should be checked
	 */
	TS_SELENIUM_BASE_URL: !process.env.TS_SELENIUM_BASE_URL ? 'http://sample-url' : process.env.TS_SELENIUM_BASE_URL.replace(/\/$/, ''),

	/**
	 * ocp infra type, possible values "PSI", "AWS", "IBM Z", "IBM Power"
	 */
	OCP_INFRA: process.env.OCP_INFRA || '',

	/**
	 * openShift version
	 */
	OCP_VERSION: process.env.OCP_VERSION || '',

	/**
	 * test environment (used as prefix in suite name)
	 */
	TEST_ENVIRONMENT: process.env.TEST_ENVIRONMENT || '',

	/**
	 * openshift project or k8s namespace which is used by test
	 */
	TEST_NAMESPACE: process.env.TEST_NAMESPACE || '',

	/**
	 * application name (DevSpaces or Che)
	 */
	TESTING_APPLICATION_NAME: (): string => {
		return BASE_TEST_CONSTANTS.TS_SELENIUM_BASE_URL.includes('devspaces')
			? 'devspaces'
			: BASE_TEST_CONSTANTS.TS_SELENIUM_BASE_URL.includes('che')
				? 'che'
				: 'default';
	},
	/**
	 * testing application version
	 */
	TESTING_APPLICATION_VERSION: process.env.TESTING_APPLICATION_VERSION || 'next',

	/**
	 * is "https://access.redhat.com/documentation/en-us/red_hat_openshift_dev_spaces/{TESTING_APPLICATION_VERSION}/" available online
	 * false by default
	 */
	IS_PRODUCT_DOCUMENTATION_RELEASED: process.env.IS_PRODUCT_DOCUMENTATION_RELEASED === 'true',

	/**
	 * is cluster disconnected of online
	 */
	IS_CLUSTER_DISCONNECTED: (): boolean => BASE_TEST_CONSTANTS.TS_SELENIUM_BASE_URL.includes('airgap'),

	/**
	 * choose the platform where "che" application deployed, "openshift" by default.
	 */
	TS_PLATFORM: process.env.TS_PLATFORM || Platform.OPENSHIFT,

	/**
	 * editor the tests are running against, "code" by default.
	 * Possible values: "che-code"
	 */
	TS_SELENIUM_EDITOR: process.env.TS_SELENIUM_EDITOR || 'che-code',

	/**
	 * file name to check if project was imported
	 */
	TS_SELENIUM_PROJECT_ROOT_FILE_NAME: process.env.TS_SELENIUM_PROJECT_ROOT_FILE_NAME || 'devfile.yaml',

	/**
	 * sample name from Dashboard to start
	 */
	TS_SELENIUM_DASHBOARD_SAMPLE_NAME: process.env.TS_SELENIUM_DASHBOARD_SAMPLE_NAME || 'Python',

	/**
	 * name of workspace created for 'Happy Path' scenario validation.
	 */
	TS_SELENIUM_HAPPY_PATH_WORKSPACE_NAME: process.env.TS_SELENIUM_HAPPY_PATH_WORKSPACE_NAME || 'EmptyWorkspace',

	/**
	 * this variable specifies that run test is used for load testing and that all artifacts will be sent to ftp client.
	 */
	TS_LOAD_TESTS: process.env.TS_LOAD_TESTS || 'false',

	/**
	 * enable Axios request interceptor, false by default
	 */
	TS_SELENIUM_REQUEST_INTERCEPTOR: process.env.TS_SELENIUM_REQUEST_INTERCEPTOR === 'true',

	/**
	 * enable Axios response interceptor, false by default
	 */
	TS_SELENIUM_RESPONSE_INTERCEPTOR: process.env.TS_SELENIUM_RESPONSE_INTERCEPTOR === 'true',

	/**
	 * stop and remove workspace if a test fails.
	 */
	DELETE_WORKSPACE_ON_FAILED_TEST: process.env.DELETE_WORKSPACE_ON_FAILED_TEST === 'true',

	/**
	 * stop and remove workspace if a test is successful.
	 */
	DELETE_WORKSPACE_ON_SUCCESSFUL_TEST: process.env.DELETE_WORKSPACE_ON_SUCCESSFUL_TEST !== 'false',

	/**
	 * restart existing workspace.
	 */
	IS_RESTART_EXISTING_WORKSPACE: process.env.IS_RESTART_EXISTING_WORKSPACE === 'true',

	/**
	 * constant, which prolong timeout constants for local debug.
	 */
	TS_DEBUG_MODE: process.env.TS_DEBUG_MODE === 'true'
};
