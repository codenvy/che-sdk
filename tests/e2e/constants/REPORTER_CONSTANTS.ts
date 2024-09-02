/** *******************************************************************
 * copyright (c) 2020-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import { BASE_TEST_CONSTANTS } from './BASE_TEST_CONSTANTS';
import { MOCHA_CONSTANTS } from './MOCHA_CONSTANTS';

export const REPORTER_CONSTANTS: {
	DELETE_SCREENCAST_IF_TEST_PASS: boolean;
	RP_ENDPOINT(): string | undefined;
	RP_IS_LOCAL_SERVER: boolean;
	REPORTERS_ENABLED(): string;
	RP_API_KEY: string;
	RP_PROJECT(): string;
	RP_RERUN(): boolean;
	RP_RERUN_UUID: string | undefined;
	RP_LAUNCH_NAME: string;
	RP_USER: string;
	RP_USE_PERSONAL: boolean;
	SAVE_ALLURE_REPORT_DATA: boolean;
	SAVE_RP_REPORT_DATA: boolean;
	TS_SELENIUM_DELAY_BETWEEN_SCREENSHOTS: number;
	TS_SELENIUM_REPORT_FOLDER: string;
	TS_SELENIUM_EXECUTION_SCREENCAST: boolean;
	TS_SELENIUM_PRINT_TIMEOUT_VARIABLES: string | boolean;
	TS_SELENIUM_LOAD_TEST_REPORT_FOLDER: string;
	TS_SELENIUM_LOG_LEVEL: string;
	TEST_RUN_URL: string;
} = {
	/**
	 * path to folder with load tests execution report.
	 */
	TS_SELENIUM_LOAD_TEST_REPORT_FOLDER: process.env.TS_SELENIUM_LOAD_TEST_REPORT_FOLDER || './load-test-folder',

	/**
	 * delay between screenshots catching in the milliseconds for the execution screencast.
	 */
	TS_SELENIUM_DELAY_BETWEEN_SCREENSHOTS: Number(process.env.TS_SELENIUM_DELAY_BETWEEN_SCREENSHOTS) || 1000,

	/**
	 * path to folder with tests execution report.
	 */
	TS_SELENIUM_REPORT_FOLDER: process.env.TS_SELENIUM_REPORT_FOLDER || './report',

	/**
	 * enable or disable storing of execution screencast, "false" by default.
	 */
	TS_SELENIUM_EXECUTION_SCREENCAST: process.env.TS_SELENIUM_EXECUTION_SCREENCAST === 'true',

	/**
	 * delete screencast after execution if all tests passed, "true" by default.
	 */
	DELETE_SCREENCAST_IF_TEST_PASS: process.env.DELETE_SCREENCAST_IF_TEST_PASS !== 'false',

	/**
	 * log level settings, possible variants: 'INFO' (by default), 'DEBUG', 'TRACE'.
	 */
	TS_SELENIUM_LOG_LEVEL: process.env.TS_SELENIUM_LOG_LEVEL || 'TRACE',

	/**
	 * print all timeout variables when tests launch, default to false
	 */
	TS_SELENIUM_PRINT_TIMEOUT_VARIABLES: process.env.TS_SELENIUM_PRINT_TIMEOUT_VARIABLES === 'true',

	/**
	 * use local Allure reporter, default to false
	 */
	SAVE_ALLURE_REPORT_DATA: process.env.SAVE_ALLURE_REPORT_DATA === 'true',

	/**
	 * use ReportPortal reporter, default to false
	 */
	SAVE_RP_REPORT_DATA: process.env.SAVE_RP_REPORT_DATA === 'true',

	/**
	 * list of enabled reporters
	 */
	REPORTERS_ENABLED: (): string => {
		let reporters: string = '';
		
		reporters += 'slnodejs/tsOutputs/mocha-reporter/index';
		reporters += ',dist/utils/CheReporter.js';

		if (REPORTER_CONSTANTS.SAVE_ALLURE_REPORT_DATA) {
			reporters += ',allure-mocha';
		}
		if (REPORTER_CONSTANTS.SAVE_RP_REPORT_DATA) {
			reporters += ',@reportportal/agent-js-mocha';
		}

		return reporters;
	},

	/**
	 * reportPortal app key or user token
	 */
	RP_API_KEY: process.env.RP_API_KEY || '',

	/**
	 * user name on ReportPortal
	 */
	RP_USER: process.env.RP_USER || process.env.USER || process.env.BUILD_USER_ID || '',

	/**
	 * launch name to save report
	 */
	RP_LAUNCH_NAME: process.env.RP_LAUNCH_NAME || `Test run ${MOCHA_CONSTANTS.MOCHA_USERSTORY}`,

	/**
	 * launch name to save report
	 */
	RP_RERUN_UUID: process.env.RP_RERUN_UUID || undefined,

	/**
	 * is launch rerun
	 */
	RP_RERUN: (): boolean => !!REPORTER_CONSTANTS.RP_RERUN_UUID,

	/**
	 * is local or online server to use
	 */
	RP_IS_LOCAL_SERVER: process.env.RP_IS_LOCAL_SERVER !== 'false',

	/**
	 * url with endpoints where ReportPortal is
	 */
	RP_ENDPOINT: (): string | undefined => {
		if (process.env.RP_ENDPOINTRP_IS_LOCAL_SERVER) {
			return 'http://localhost:8080/api/v1';
		}
		if (process.env.RP_ENDPOINT !== '') {
			return process.env.RP_ENDPOINT;
		}
		return 'https://reportportal-crw.apps.ocp-c1.prod.psi.redhat.com/api/v1';
	},

	/**
	 * use personal project to save launch, if false launch will be sent to devspaces or che project, true by default
	 */
	RP_USE_PERSONAL: process.env.RP_USE_PERSONAL !== 'false',

	/**
	 * project name to save launch
	 */
	RP_PROJECT: (): string => {
		const project: string = REPORTER_CONSTANTS.RP_USE_PERSONAL
			? `${REPORTER_CONSTANTS.RP_USER}_personal`
			: BASE_TEST_CONSTANTS.TESTING_APPLICATION_NAME();
		return process.env.RP_PROJECT || project;
	},

	TEST_RUN_URL: process.env.TEST_RUN_URL || 'Test run url not set'
};
