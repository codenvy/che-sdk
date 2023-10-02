#!/bin/bash
# shellcheck source=/dev/null

validateParameters(){
# Validate required parameters
  if [ -z "$OCP_VERSION" ] || [ -z "$ARCH_VERSION" ] || [ -z "$TS_SELENIUM_BASE_URL" ]; then
      echo "The ARCH_VERSION, OCP_VERSION or TS_SELENIUM_BASE_URL is not set!";
      echo "Please, set all required environment variable parameters"
      exit 1
  fi
}

########################################
############# Methods ##################
########################################

launchAPITests() {
  export MOCHA_SUITE="APITest"
  echo "MOCHA_SUITE = ${MOCHA_SUITE}"
  export RP_LAUNCH_NAME="API tests suite"
  echo "suites/$OCP_TYPE/$MOCHA_SUITE"
  npm run driver-less-test
}

launchDynamicallyGeneratingAPITests() {
  export MOCHA_SUITE="DynamicallyGeneratingAPITest"
  export RP_LAUNCH_NAME="Application inbuilt DevWorkspaces API tests suite"
  echo "MOCHA_SUITE = ${MOCHA_SUITE}"
  echo "suites/$OCP_TYPE/$MOCHA_SUITE"
  npm run delayed-test
}

launchUITests() {
  export MOCHA_SUITE="UITest"
  export RP_LAUNCH_NAME="UI tests suite"
  echo "MOCHA_SUITE = ${MOCHA_SUITE}"
  echo "suites/$OCP_TYPE/$MOCHA_SUITE"
  npm run test
}

launchAllTests() {
  validateParameters
  initTestValues
  echo ""
  echo "Launching all tests for $TEST_ENVIRONMENT"
  echo ""
  launchDynamicallyGeneratingAPITests
  launchAPITests
  launchUITests
}

initTestValues() {
  if [[ "$TS_SELENIUM_BASE_URL" =~ "airgap" ]]
  then
     echo "Disconnected environment"
     export OCP_TYPE="disconnected-ocp"
  else
     echo "Online environment"
     export OCP_TYPE="online-ocp"
  fi

  export TEST_ENVIRONMENT="$ARCH_VERSION $OCP_TYPE $OCP_VERSION"
  export DELETE_WORKSPACE_ON_FAILED_TEST=${DELETE_WORKSPACE_ON_FAILED_TEST:-'false'}
  export DELETE_SCREENCAST_IF_TEST_PASS=${DELETE_SCREENCAST_IF_TEST_PASS:-'true'}
  export NODE_TLS_REJECT_UNAUTHORIZED=${NODE_TLS_REJECT_UNAUTHORIZED:-'0'}
  export TS_OCP_LOGIN_PAGE_PROVIDER_TITLE=${TS_OCP_LOGIN_PAGE_PROVIDER_TITLE:-'htpasswd'}
  export TS_SELENIUM_DELAY_BETWEEN_SCREENSHOTS=${TS_SELENIUM_DELAY_BETWEEN_SCREENSHOTS:-'1000'}
  export TS_SELENIUM_EDITOR=${TS_SELENIUM_EDITOR:-'che-code'}
  export TS_SELENIUM_EXECUTION_SCREENCAST=${TS_SELENIUM_EXECUTION_SCREENCAST:-'false'}
  export TS_SELENIUM_HEADLESS=${TS_SELENIUM_HEADLESS:-'false'}
  export TS_SELENIUM_LAUNCH_FULLSCREEN=${TS_SELENIUM_LAUNCH_FULLSCREEN:-'true'}
  export TS_SELENIUM_LOG_LEVEL=${TS_SELENIUM_LOG_LEVEL:-'TRACE'}
  export TS_SELENIUM_OCP_PASSWORD=${TS_SELENIUM_OCP_PASSWORD:-'nopasswd'}
  export TS_SELENIUM_OCP_USERNAME=${TS_SELENIUM_OCP_USERNAME:-'admin'}
  export TS_SELENIUM_VALUE_OPENSHIFT_OAUTH=${TS_SELENIUM_VALUE_OPENSHIFT_OAUTH:-'true'}
  export TS_SELENIUM_REPORT_FOLDER=${TS_SELENIUM_REPORT_FOLDER:-'./report'}
  export MOCHA_BAIL=${MOCHA_BAIL:-'false'}
  export MOCHA_RETRIES=${MOCHA_RETRIES:-'1'}

  echo "TS_SELENIUM_BASE_URL=${TS_SELENIUM_BASE_URL}"
  echo "TEST_ENVIRONMENT=${TEST_ENVIRONMENT}"
  echo "DELETE_WORKSPACE_ON_FAILED_TEST=${DELETE_WORKSPACE_ON_FAILED_TEST}"
  echo "DELETE_SCREENCAST_IF_TEST_PASS=${DELETE_SCREENCAST_IF_TEST_PASS}"
  echo "NODE_TLS_REJECT_UNAUTHORIZED=${NODE_TLS_REJECT_UNAUTHORIZED}"
  echo "TS_OCP_LOGIN_PAGE_PROVIDER_TITLE=${TS_OCP_LOGIN_PAGE_PROVIDER_TITLE}"
  echo "TS_SELENIUM_DELAY_BETWEEN_SCREENSHOTS=${TS_SELENIUM_DELAY_BETWEEN_SCREENSHOTS}"
  echo "TS_SELENIUM_EDITOR=${TS_SELENIUM_EDITOR}"
  echo "TS_SELENIUM_EXECUTION_SCREENCAST=${TS_SELENIUM_EXECUTION_SCREENCAST}"
  echo "TS_SELENIUM_HEADLESS=${TS_SELENIUM_HEADLESS}"
  echo "TS_SELENIUM_LAUNCH_FULLSCREEN=${TS_SELENIUM_LAUNCH_FULLSCREEN}"
  echo "TS_SELENIUM_LOG_LEVEL=${TS_SELENIUM_LOG_LEVEL}"
  echo "TS_SELENIUM_OCP_USERNAME=${TS_SELENIUM_OCP_USERNAME}"
  echo "TS_SELENIUM_VALUE_OPENSHIFT_OAUTH=${TS_SELENIUM_VALUE_OPENSHIFT_OAUTH}"
  echo "TS_SELENIUM_REPORT_FOLDER=${TS_SELENIUM_REPORT_FOLDER}"
  echo "MOCHA_BAIL=${MOCHA_BAIL}"
  echo "MOCHA_RETRIES=${MOCHA_RETRIES}"
}

########################################
############# Launching ################
########################################

launchAllTests

