#!/bin/bash
# Copyright (c) 2016 Codenvy, S.A.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

pre_init() {
  ADDITIONAL_MANDATORY_PARAMETERS=""
  ADDITIONAL_OPTIONAL_DOCKER_PARAMETERS="
  -e CHE_HOST=<YOUR_HOST>              IP address or hostname where che will serve its users
  -e CHE_PORT=<YOUR_PORT>              Port where che will bind itself to
  -e CHE_CONTAINER=<YOUR_NAME>         Name for the che container"
  ADDITIONAL_OPTIONAL_DOCKER_MOUNTS=""
  ADDITIONAL_COMMANDS=""
  ADDITIONAL_GLOBAL_OPTIONS=""
}

source /scripts/base/startup.sh
start "$@"

