/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.core.jsonrpc.commons;

import com.google.inject.Singleton;

import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Transmits json rpc requests containing DTO objects. The naming of methods
 * explicitly corresponds to what you want to send and to receive. Generally
 * method name looks like 'transmitXtoY' where X and Y can be none, one or
 * many. When we say 'transmitManyToOne' we mean that we want to transmit a
 * request that has parameters represented as a list (many) and receive a
 * result that is represented as a single value (one).
 * <p>
 * Note: if you need to transmit or receive {@link String}, {@link Boolean},
 * {@link Double} it is recommended to us {@link BuildingRequestTransmitter}
 * instance as it provides more general facilities for that.
 */
@Singleton
public class RequestTransmitter {
    private final static Logger LOGGER = getLogger(RequestTransmitter.class);

    private final BuildingRequestTransmitter transmitter;

    @Inject
    public RequestTransmitter(BuildingRequestTransmitter transmitter) {
        this.transmitter = transmitter;
    }

    private static <P> void checkParamsValue(P pValue) {
        checkNotNull(pValue, "Params value must not be null");
    }

    private static <P> void checkParamsValue(List<P> pValue) {
        checkNotNull(pValue, "Params value must not be null");
        checkArgument(!pValue.isEmpty(), "Params list must not be empty");
    }

    private static void checkMethodName(String method) {
        checkNotNull(method, "Method name must not be null");
        checkArgument(!method.isEmpty(), "Method name must not be empty");
    }

    private static void checkEndpointId(String endpointId) {
        checkNotNull(endpointId, "Endpoint ID must not be null");
        checkArgument(!endpointId.isEmpty(), "Endpoint ID must not be empty");
    }

    private static <R> void checkResultClass(Class<R> rClass) {
        checkNotNull(rClass, "Result class must not be null");
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
        checkEndpointId(endpointId);
        checkMethodName(method);

        LOGGER.debug("Initiating a transmission of a notification: " +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method);

        transmitter.newRequest()
                   .endpointId(endpointId)
                   .methodName(method)
                   .noParams()
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
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkParamsValue(pValue);

        LOGGER.debug("Initiating a transmission of a notification:" +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "params: " + pValue);

        transmitter.newRequest()
                   .endpointId(endpointId)
                   .methodName(method)
                   .paramsAsDto(pValue)
                   .sendAndSkipResult();

    }

    /**
     * Transmit a notification with params as a single string
     *
     * @param endpointId
     *         endpoint to address a transmission
     * @param method
     *         method name to address a transmission
     * @param pValue
     *         params value
     */
    public void transmitStringToNone(String endpointId, String method, String pValue) {
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkParamsValue(pValue);

        LOGGER.debug("Initiating a transmission of a notification:" +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "string: " + pValue);

        transmitter.newRequest()
                   .endpointId(endpointId)
                   .methodName(method)
                   .paramsAsString(pValue)
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
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkParamsValue(pValue);

        LOGGER.debug("Initiating a transmission of a notification:" +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "params list: " + pValue);

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
     * @return
     */
    public <R> JsonRpcPromise<R> transmitNoneToOne(String endpointId, String method, Class<R> rClass) {
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkResultClass(rClass);

        LOGGER.debug("Initiating a transmission of a request:" +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "result class: " + rClass);

        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .noParams()
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
     * @return
     */
    public <R> JsonRpcPromise<List<R>> transmitNoneToMany(String endpointId, String method, Class<R> rClass) {
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkResultClass(rClass);

        LOGGER.debug("Initiating a transmission of a request: " +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "result list class: " + rClass);

        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .noParams()
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
     * @return
     */
    public <P, R> JsonRpcPromise<R> transmitOneToOne(String endpointId, String method, P pValue, Class<R> rClass) {
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkParamsValue(pValue);
        checkResultClass(rClass);

        LOGGER.debug("Initiating a transmission of a request: " +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "params object class: " + pValue.getClass() + ", " +
                     "params object value: " + pValue + ", " +
                     "result object class: " + rClass);

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
     * @return
     */
    public <P, R> JsonRpcPromise<R> transmitManyToOne(String endpointId, String method, List<P> pValue, Class<R> rClass) {
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkParamsValue(pValue);
        checkResultClass(rClass);

        LOGGER.debug("Initiating a transmission of a request: " +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "params list item class: " + pValue.iterator().next().getClass() + ", " +
                     "params list value: " + pValue + ", " +
                     "result object class: " + rClass);

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
     * @return
     */
    public <P, R> JsonRpcPromise<List<R>> transmitOneToMany(String endpointId, String method, P pValue, Class<R> rClass) {
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkParamsValue(pValue);
        checkResultClass(rClass);

        LOGGER.debug("Initiating a transmission of a request: " +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "params object class: " + pValue.getClass() + ", " +
                     "params object value: " + pValue + ", " +
                     "result list class: " + rClass);

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
     * @return
     */
    public <P, R> JsonRpcPromise<List<R>> transmitManyToMany(String endpointId, String method, List<P> pValue, Class<R> rClass) {
        checkEndpointId(endpointId);
        checkMethodName(method);
        checkResultClass(rClass);

        LOGGER.debug("Initiating a transmission of a request: " +
                     "endpoint ID: " + endpointId + ", " +
                     "method: " + method + ", " +
                     "params list item class: " + pValue.iterator().next().getClass() + ", " +
                     "params list value: " + pValue + ", " +
                     "result list class: " + rClass);

        return transmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsListOfDto(pValue)
                          .sendAndReceiveResultAsListOfDto(rClass);
    }
}
