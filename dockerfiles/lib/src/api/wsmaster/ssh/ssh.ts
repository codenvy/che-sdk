/*
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc.- initial API and implementation
 */

import {org} from "../../dto/che-dto"
import {AuthData} from "../auth/auth-data";
import {HttpJsonRequest} from "../../../spi/http/default-http-json-request";
import {DefaultHttpJsonRequest} from "../../../spi/http/default-http-json-request";
import {HttpJsonResponse} from "../../../spi/http/default-http-json-request";
import {ServerLocation} from "../../../utils/server-location";

/**
 * SSh class allowing to manage all ssh operations
 * @author Florent Benoit
 */
export class Ssh {

    /**
     * Authentication data
     */
    authData:AuthData;

    /**
     * Location of API server
     */
    apiLocation : ServerLocation;

    constructor(authData:AuthData, apiLocation : ServerLocation) {
        this.authData = authData;
        this.apiLocation = apiLocation;
    }


    /**
     * Gets ssh pair by service and name.
     *
     * @param service
     *         service name of ssh pair
     * @param name
     *         name of ssh pair
     * @return instance of ssh pair
     * @throws NotFoundException
     *         when ssh pair is not found
     * @throws ServerException
     *         when any other error occurs during ssh pair fetching
     */
    getPair(service: string, name: string):Promise<org.eclipse.che.api.ssh.shared.dto.SshPairDto> {
        let jsonRequest: HttpJsonRequest = new DefaultHttpJsonRequest(this.authData, this.apiLocation, '/api/ssh/' + service + '/find?name=' + name, 200);
        return jsonRequest.request().then((jsonResponse:HttpJsonResponse) => {
            return jsonResponse.asDto(org.eclipse.che.api.ssh.shared.dto.SshPairDtoImpl);
        });
    }

}
