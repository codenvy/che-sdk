#!/bin/bash
# Copyright (c) 2012-2017 Red Hat, Inc
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#

set -e

init() {
LOCAL_IP_ADDRESS=$(detectIP)
#OS specific defaults
if [[ "$OSTYPE" == "darwin"* ]]; then
    DEFAULT_OC_PUBLIC_HOSTNAME="$LOCAL_IP_ADDRESS"
    DEFAULT_OC_PUBLIC_IP="$LOCAL_IP_ADDRESS"
    DEFAULT_OC_BINARY_DOWNLOAD_URL="https://github.com/openshift/origin/releases/download/v3.6.0/openshift-origin-client-tools-v3.6.0-c4dd4cf-mac.zip"
    DEFAULT_JQ_BINARY_DOWNLOAD_URL="https://github.com/stedolan/jq/releases/download/jq-1.5/jq-osx-amd64"
else
    DEFAULT_OC_PUBLIC_HOSTNAME="$LOCAL_IP_ADDRESS"
    DEFAULT_OC_PUBLIC_IP="$LOCAL_IP_ADDRESS"
    DEFAULT_OC_BINARY_DOWNLOAD_URL="https://github.com/openshift/origin/releases/download/v3.6.0/openshift-origin-client-tools-v3.6.0-c4dd4cf-linux-64bit.tar.gz"
    DEFAULT_JQ_BINARY_DOWNLOAD_URL="https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64"
fi

export OC_PUBLIC_HOSTNAME=${OC_PUBLIC_HOSTNAME:-${DEFAULT_OC_PUBLIC_HOSTNAME}}
export OC_PUBLIC_IP=${OC_PUBLIC_IP:-${DEFAULT_OC_PUBLIC_IP}}

export OC_BINARY_DOWNLOAD_URL=${OC_BINARY_DOWNLOAD_URL:-${DEFAULT_OC_BINARY_DOWNLOAD_URL}}
export JQ_BINARY_DOWNLOAD_URL=${JQ_BINARY_DOWNLOAD_URL:-${DEFAULT_JQ_BINARY_DOWNLOAD_URL}}

DEFAULT_CHE_MULTIUSER="false"
export CHE_MULTIUSER=${CHE_MULTIUSER:-${DEFAULT_CHE_MULTIUSER}}

DEFAULT_OPENSHIFT_USERNAME="developer"
export OPENSHIFT_USERNAME=${OPENSHIFT_USERNAME:-${DEFAULT_OPENSHIFT_USERNAME}}

DEFAULT_OPENSHIFT_PASSWORD="developer"
export OPENSHIFT_PASSWORD=${OPENSHIFT_PASSWORD:-${DEFAULT_OPENSHIFT_PASSWORD}}

DEFAULT_OPENSHIFT_NAMESPACE_URL="eclipse-che.${OC_PUBLIC_IP}.nip.io"
export OPENSHIFT_NAMESPACE_URL=${OPENSHIFT_NAMESPACE_URL:-${DEFAULT_OPENSHIFT_NAMESPACE_URL}}

export OPENSHIFT_FLAVOR="ocp"

DEFAULT_OPENSHIFT_ENDPOINT="https://${OC_PUBLIC_HOSTNAME}:8443"
export OPENSHIFT_ENDPOINT=${OPENSHIFT_ENDPOINT:-${DEFAULT_OPENSHIFT_ENDPOINT}}

DEFAULT_ENABLE_SSL="false"
export ENABLE_SSL=${ENABLE_SSL:-${DEFAULT_ENABLE_SSL}}

DEFAULT_CHE_IMAGE_TAG="nightly"
export CHE_IMAGE_TAG=${CHE_IMAGE_TAG:-${DEFAULT_CHE_IMAGE_TAG}}

DEFAULT_IMAGE_PULL_POLICY="Always"
export IMAGE_PULL_POLICY=${IMAGE_PULL_POLICY:-${DEFAULT_IMAGE_PULL_POLICY}}

DEFAULT_CHE_IMAGE_REPO="eclipse/che-server"
export CHE_IMAGE_REPO=${CHE_IMAGE_REPO:-${DEFAULT_CHE_IMAGE_REPO}}

DEFAULT_IMAGE_INIT="eclipse/che-init"
export IMAGE_INIT=${IMAGE_INIT:-${DEFAULT_IMAGE_INIT}}:${CHE_IMAGE_TAG}

DEFAULT_CONFIG_DIR="/tmp/che-config"
export CONFIG_DIR=${CONFIG_DIR:-${DEFAULT_CONFIG_DIR}}

}

get_tools() {
    TOOLS_DIR="/tmp"
    OC_BINARY="$TOOLS_DIR/oc"
    JQ_BINARY="$TOOLS_DIR/jq"
    #OS specific extract archives
    if [[ "$OSTYPE" == "darwin"* ]]; then
        OC_PACKAGE="openshift-origin-client-tools.zip"
        ARCH="unzip -d $TOOLS_DIR"
        EXTRA_ARGS=""
    else
        OC_PACKAGE="openshift-origin-client-tools.tar.gz"
        ARCH="tar --strip 1 -xzf"
        EXTRA_ARGS="-C $TOOLS_DIR"
    fi

    if [ ! -f $OC_BINARY ]; then
        echo "download oc client..."
        wget -q -O $TOOLS_DIR/$OC_PACKAGE $OC_BINARY_DOWNLOAD_URL
        eval "$ARCH" "$TOOLS_DIR"/"$OC_PACKAGE" "$EXTRA_ARGS" &>/dev/null
        rm -rf "$TOOLS_DIR"/README.md "$TOOLS_DIR"/LICENSE "${TOOLS_DIR:-/tmp}"/"$OC_PACKAGE"
    fi

    if [ ! -f $JQ_BINARY ]; then
        echo "download jq..."
        wget -q -O $JQ_BINARY $JQ_BINARY_DOWNLOAD_URL
        chmod +x $JQ_BINARY
    fi
    export PATH=${PATH}:${TOOLS_DIR}
}

ocp_is_booted() {
    # we have to wait before docker registry will be started as it is staring as last container and it should be running before we perform che deploy.
    ocp_registry_container_id=$(docker ps -a  | grep openshift/origin-docker-registry | cut -d ' ' -f1)
    if [ ! -z "$ocp_registry_container_id" ];then
        ocp_registry_container_status=$(docker inspect "$ocp_registry_container_id" | $JQ_BINARY .[0] | $JQ_BINARY -r '.State.Status')
    else
        return 1
    fi
    if [[ "${ocp_registry_container_status}" == "running" ]]; then
        return 0
    else
        return 1
    fi
}

wait_ocp() {
  OCP_BOOT_TIMEOUT=120
  echo "[OCP] wait for ocp full boot..."
  ELAPSED=0
  until ocp_is_booted; do
    if [ ${ELAPSED} -eq "${OCP_BOOT_TIMEOUT}" ];then
        echo "OCP didn't started in $OCP_BOOT_TIMEOUT secs, exit"
        exit 1
    fi
    sleep 2
    ELAPSED=$((ELAPSED+1))
  done
}

run_ocp() {
    $OC_BINARY cluster up --public-hostname="${OC_PUBLIC_HOSTNAME}" --routing-suffix="${OC_PUBLIC_IP}.nip.io"
    wait_ocp
}

deploy_che_to_ocp() {
    #Repull init image only if DEFAULT_IMAGE_PULL_POLICY is set to Always
    if [ $DEFAULT_IMAGE_PULL_POLICY == "Always" ]; then
        docker pull "$IMAGE_INIT"
    fi
    docker run -t --rm -v /var/run/docker.sock:/var/run/docker.sock -v "${CONFIG_DIR}":/data -e IMAGE_INIT="$IMAGE_INIT" -e CHE_MULTIUSER="$CHE_MULTIUSER" eclipse/che-cli:${CHE_IMAGE_TAG} destroy --quiet --skip:pull --skip:nightly
    docker run -t --rm -v /var/run/docker.sock:/var/run/docker.sock -v "${CONFIG_DIR}":/data -e IMAGE_INIT="$IMAGE_INIT" -e CHE_MULTIUSER="$CHE_MULTIUSER" eclipse/che-cli:${CHE_IMAGE_TAG} config --skip:pull --skip:nightly
    cd "${CONFIG_DIR}/instance/config/openshift/scripts/"
    bash deploy_che.sh
    wait_until_server_is_booted
}

server_is_booted() {
  PING_URL="http://che-$OPENSHIFT_NAMESPACE_URL"
  HTTP_STATUS_CODE=$(curl -I -k "${PING_URL}" -s -o /dev/null --write-out '%{http_code}')
  if [[ "${HTTP_STATUS_CODE}" = "200" ]] || [[ "${HTTP_STATUS_CODE}" = "302" ]]; then
    return 0
  else
    return 1
  fi
}

wait_until_server_is_booted() {
  SERVER_BOOT_TIMEOUT=300
  echo "[CHE] wait CHE pod booting..."
  ELAPSED=0
  until server_is_booted || [ ${ELAPSED} -eq "${SERVER_BOOT_TIMEOUT}" ]; do
    sleep 2
    ELAPSED=$((ELAPSED+1))
  done
}

destroy_ocp() {
    $OC_BINARY login -u system:admin
    $OC_BINARY delete pvc --all
    $OC_BINARY delete all --all
    $OC_BINARY cluster down
}

detectIP() {
    docker run --rm --net host eclipse/che-ip:nightly
}

parse_args() {
    HELP="valid args: \\n
    --run-ocp - run ocp cluster\\n
    --destroy - destroy ocp cluster \\n
    --deploy-che - deploy che to ocp \\n
    --multiuser - deploy che in multiuser mode \\n
    =================================== \\n
    ENV vars \\n
    CHE_IMAGE_TAG - set CHE images tag, default: nightly \\n
    CHE_MULTIUSER - set CHE multi user mode, default: false (single user) \\n
"



    if [ $# -eq 0 ]; then
        echo "No arguments supplied"
        echo -e "$HELP"
        exit 1
    fi

    if [[ "$@" == *"--multiuser"* ]]; then
      CHE_MULTIUSER=true
    fi


    for i in "${@}"
    do
        case $i in
           --run-ocp)
               run_ocp
               shift
           ;;
           --destroy)
               destroy_ocp
               shift
           ;;
           --deploy-che)
               deploy_che_to_ocp
               shift
           ;;
        esac
    done
}

init
get_tools
parse_args "$@"
