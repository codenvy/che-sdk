/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.jsonrpc;

import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Promise;

import javax.inject.Inject;
import java.util.List;

/**
 * Transmits json rpc requests containing DTO objects.
 *
 * Note: if you need to transmit or receive {@link String}, {@link Boolean},
 * {@link Double} use {@link BuildingRequestTransmitter} instance as it
 * provides more general facilities for that.
 */
@Singleton
public class RequestTransmitter {
    private final BuildingRequestTransmitter transmitter;

    @Inject
    public RequestTransmitter(BuildingRequestTransmitter transmitter) {
        this.transmitter = transmitter;
    }

    /**
     * Transmit a notification with empty params section
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     */
    public void transmitNoneToNone(String endpointId, String method) {
        transmitter.newRequest()
                   .endpointId(endpointId)
                   .methodName(method)
                   .paramsAsEmpty()
                   .sendAndSkipResult();
    }

    /**
     * Transmit a notification with params as a single object
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param pValue
     *         params value
     * @param <P>
     *         params class
     */
    public <P> void transmitOneToNone(String endpointId, String method, P pValue) {
        transmitter.newRequest()
                   .endpointId(endpointId)
                   .methodName(method)
                   .paramsAsDto(pValue)
                   .sendAndSkipResult();

    }

    /**
     * Transmit a notification with params as a list of objects
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param pValue
     *         params value
     * @param <P>
     *         params class
     */
    public <P> void transmitManyToNone(String endpointId, String method, List<P> pValue) {
        transmitter.newRequest()
                   .endpointId(endpointId)
                   .methodName(method)
                   .paramsAsListOfDto(pValue)
                   .sendAndSkipResult();
    }

    /**
     * Transmit a request with an empty params section and a result as a single object
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param rClass
     *         result class
     * @param <R>
     *         result class
     *
     * @return
     */
    public <R> Promise<R> transmitNoneToOne(String endpointId, String method, Class<R> rClass) {
        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsEmpty()
                          .sendAndReceiveResultAsDto(rClass);
    }

    /**
     * Transmit a request with an empty params section and a result as a list of objects
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param rClass
     *         result class
     * @param <R>
     *         result class
     *
     * @return
     */
    public <R> Promise<List<R>> transmitNoneToMany(String endpointId, String method, Class<R> rClass) {
        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsEmpty()
                          .sendAndReceiveResultAsListOfDto(rClass);
    }

    /**
     * Transmit a request with params as a single object and result as a single object
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param pValue
     *         params value
     * @param rClass
     *         result class
     * @param <P>
     *         params class
     * @param <R>
     *         result class
     *
     * @return
     */
    public <P, R> Promise<R> transmitOneToOne(String endpointId, String method, P pValue, Class<R> rClass) {
        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsDto(pValue)
                          .sendAndReceiveResultAsDto(rClass);
    }

    /**
     * Transmit a request with params as s list of objects and result as a single object
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param pValue
     *         params value
     * @param rClass
     *         result class
     * @param <P>
     *         params class
     * @param <R>
     *         result class
     *
     * @return
     */
    public <P, R> Promise<R> transmitManyToOne(String endpointId, String method, List<P> pValue, Class<R> rClass) {
        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsListOfDto(pValue)
                          .sendAndReceiveResultAsDto(rClass);
    }

    /**
     * Transmit a request with params as a single object and result as a list of objects
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param pValue
     *         params value
     * @param rClass
     *         result class
     * @param <P>
     *         params class
     * @param <R>
     *         result class
     *
     * @return
     */
    public <P, R> Promise<List<R>> transmitOneToMany(String endpointId, String method, P pValue, Class<R> rClass) {
        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsDto(pValue)
                          .sendAndReceiveResultAsListOfDto(rClass);
    }

    /**
     * Transmit a request with params as a list of objects and result as a list of objects
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param pValue
     *         params value
     * @param rClass
     *         result class
     * @param <P>
     *         params class
     * @param <R>
     *         result class
     *
     * @return
     */
    public <P, R> Promise<List<R>> transmitManyToMany(String endpointId, String method, List<P> pValue, Class<R> rClass) {
        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsListOfDto(pValue)
                          .sendAndReceiveResultAsListOfDto(rClass);
    }
}
