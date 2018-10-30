#!/bin/sh
#
# Copyright (c) 2018-2018 Red Hat, Inc.
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#   Red Hat, Inc. - initial API and implementation
#

echo "Use production mode"

# Size before
SIZE_BEFORE=$(du -s . | cut -f1)
LAST_SIZE=${SIZE_BEFORE}

cd ${HOME}
yarn install --frozen-lockfile --no-cache --production

# Size after
SIZE_AFTER=$(du -s . | cut -f1)

echo "Current gain $((${SIZE_BEFORE}-${SIZE_AFTER}))"
