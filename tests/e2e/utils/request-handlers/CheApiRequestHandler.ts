/*********************************************************************
 * Copyright (c) 2019-2023 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import { TYPES } from '../../configs/inversify.types';
import { inject, injectable } from 'inversify';
import { IAuthorizationHeaderHandler } from './headers/IAuthorizationHeaderHandler';
import { Logger } from '../Logger';
import { BaseTestConstants } from '../../constants/BaseTestConstants';

@injectable()
export class CheApiRequestHandler {

  /**
   * This method adds a request interceptor into axios request interceptors list and returns an ID of the interceptor
   */
  static enableRequestInterceptor(): number {
    Logger.debug();
    return axios.interceptors.request.use(request => {
      try {
        let request_censored: AxiosRequestConfig = JSON.parse(JSON.stringify(request));
        if (request_censored === undefined) {
          Logger.error('JSON.parse returned an undefined object, cannot process request');
          return request;
        }
        if (request_censored.headers === undefined) {
          Logger.warn('request does not contain any headers object');
          return request;
        }
        request_censored.headers.Authorization = 'CENSORED';
        request_censored.headers.Cookie = 'CENSORED';
        Logger.info(`request:\n` + request_censored);
      } catch (err) {
        Logger.error(`request: Failed to deep clone AxiosRequestConfig:` + err);
      }
      return request;
    });
  }

  /**
   * This method adds a response interceptor into axios response interceptors list and returns an ID of the interceptor
   */
  static enableResponseInterceptor(): number {
    Logger.debug();
    return axios.interceptors.response.use(response => {
      try {
        let response_censored: AxiosResponse = JSON.parse(JSON.stringify(response, (key, value) => {
          switch (key) {
            case 'request':
              return 'CENSORED';
            default:
              return value;
          }
        }));
        if (response_censored === undefined) {
          Logger.error('JSON.parse returned an undefined object, cannot process response');
          return response;
        }
        if (response_censored.config === undefined) {
          Logger.warn('response does not contain any config object');
          return response;
        }
        if (response_censored.config.headers === undefined) {
          Logger.warn('response does not contain any config.headers object');
          return response;
        }
        response_censored.config.headers.Authorization = 'CENSORED';
        response_censored.config.headers.Cookie = 'CENSORED';
        if (response_censored.data.access_token !== null) {
          response_censored.data.access_token = 'CENSORED';
        }
        if (response_censored.data.refresh_token !== null) {
          response_censored.data.refresh_token = 'CENSORED';
        }
        Logger.info(`response:\n` + response_censored);
      } catch (err) {
        Logger.error(`response: Failed to deep clone AxiosResponse:` + err);
      }
      return response;
    });
  }

  constructor(@inject(TYPES.IAuthorizationHeaderHandler) private readonly headerHandler: IAuthorizationHeaderHandler) { }

  async get(relativeUrl: string): Promise<AxiosResponse> {
    return await axios.get(this.assembleUrl(relativeUrl), await this.headerHandler.get());
  }

  async post(relativeUrl: string, data?: string | any): Promise<AxiosResponse> {
    return await axios.post(this.assembleUrl(relativeUrl), data, await this.headerHandler.get());
  }

  async delete(relativeUrl: string): Promise<AxiosResponse> {
    return await axios.delete(this.assembleUrl(relativeUrl), await this.headerHandler.get());
  }

  async patch(relativeUrl: string, patchParams: object): Promise<AxiosResponse> {
    return await axios.patch(this.assembleUrl(relativeUrl), patchParams, await this.headerHandler.get());
  }

  private assembleUrl(relativeUrl: string): string {
    return `${BaseTestConstants.TS_SELENIUM_BASE_URL}/${relativeUrl}`;
  }

}
