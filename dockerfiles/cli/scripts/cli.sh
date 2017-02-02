#!/bin/bash
# Copyright (c) 2016 Codenvy, S.A.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

post_init() {
  GLOBAL_HOST_IP=${GLOBAL_HOST_IP:=$(docker_run --net host ${BOOTSTRAP_IMAGE_CHEIP})}
  DEFAULT_CHE_HOST=$GLOBAL_HOST_IP
  CHE_HOST=${CHE_HOST:-${DEFAULT_CHE_HOST}}
  DEFAULT_CHE_PORT=8080
  CHE_PORT=${CHE_PORT:-${DEFAULT_CHE_PORT}}
  CHE_MIN_RAM=1.5
  CHE_MIN_DISK=100

  DEFAULT_CHE_SERVER_CONTAINER_NAME="${CHE_MINI_PRODUCT_NAME}"
  CHE_SERVER_CONTAINER_NAME="${CHE_SERVER_CONTAINER_NAME:-${DEFAULT_CHE_SERVER_CONTAINER_NAME}}"
 
  DEFAULT_CHE_CONTAINER_NAME="${CHE_SERVER_CONTAINER_NAME}"
  CHE_CONTAINER_NAME="${CHE_CONTAINER:-${DEFAULT_CHE_CONTAINER_NAME}}"

  DEFAULT_CHE_CONTAINER_PREFIX="${CHE_SERVER_CONTAINER_NAME}"
  CHE_CONTAINER_PREFIX="${CHE_CONTAINER_PREFIX:-${DEFAULT_CHE_CONTAINER_PREFIX}}"

  if [[ "${CHE_CONTAINER_NAME}" = "${CHE_MINI_PRODUCT_NAME}" ]]; then 	
  	if [[ "${CHE_PORT}" != "${DEFAULT_CHE_PORT}" ]]; then
  	  CHE_CONTAINER_NAME="${CHE_CONTAINER_PREFIX}-${CHE_PORT}"
  	else 
  	  CHE_CONTAINER_NAME="${CHE_CONTAINER_PREFIX}"
  	fi
  fi
}
