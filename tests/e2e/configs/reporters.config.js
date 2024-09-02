/** *******************************************************************
 * copyright (c) 2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
const { REPORTER_CONSTANTS } = require('../constants/REPORTER_CONSTANTS');
const { BASE_TEST_CONSTANTS } = require('../constants/BASE_TEST_CONSTANTS');

module.exports = {
	reporterEnabled: REPORTER_CONSTANTS.REPORTERS_ENABLED(),
	seaLightsReporterReporterOptions: {},
	allureMochaReporterOptions: {
		resultsDir: '.allure-results'
	},
	reportportalAgentJsMochaReporterOptions: {
		apiKey: REPORTER_CONSTANTS.RP_API_KEY,
		endpoint: REPORTER_CONSTANTS.RP_ENDPOINT(),
		project: REPORTER_CONSTANTS.RP_PROJECT(),
		launch: `${REPORTER_CONSTANTS.RP_LAUNCH_NAME}`,
		attributes: [
			{
				key: 'build',
				value: `${BASE_TEST_CONSTANTS.TESTING_APPLICATION_VERSION}`
			},
			{
				key: 'ocp version',
				value: `${BASE_TEST_CONSTANTS.OCP_VERSION}`
			},
			{
				key: 'ocp infra',
				value: `${BASE_TEST_CONSTANTS.OCP_INFRA}`
			},
			{
				key: 'application',
				value: BASE_TEST_CONSTANTS.TESTING_APPLICATION_NAME()
			},
			{
				key: 'url',
				value: BASE_TEST_CONSTANTS.TS_SELENIUM_BASE_URL
			},
			{
				value: REPORTER_CONSTANTS.TEST_RUN_URL
			}
		],
		rerun: REPORTER_CONSTANTS.RP_RERUN(),
		rerunOf: REPORTER_CONSTANTS.RP_RERUN_UUID,
		restClientConfig: {
			timeout: 1200000
		}
	}
};
