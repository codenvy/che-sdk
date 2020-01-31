import { TestConstants, RequestType } from '..';
import axios from 'axios';
import querystring from 'querystring';
import { injectable } from 'inversify';

@injectable()
export class RequestHandler {
    async processRequest(reqType: RequestType, url: string, data?: string) {
        let response;
        // maybe this check can be moved somewhere else at the begining so it will be executed just once
        if (TestConstants.TS_SELENIUM_MULTIUSER === true) {
            let authorization = 'Authorization';
            axios.defaults.headers.common[authorization] = 'Bearer ' + await this.getCheBearerToken();
        }
        switch (reqType) {
            case RequestType.GET: {
                response = await axios.get(url);
                break;
            }
            case RequestType.DELETE: {
                response = await axios.delete(url);
                break;
            }
            case RequestType.POST: {
                if ( data === undefined ) {
                    response = axios.post(url);
                } else {
                    response = axios.post(url, data);
                }
                break;
            }
            default: {
                throw new Error('Unknown RequestType: ' + reqType);
            }
        }
        return response;
    }

    async getCheBearerToken(): Promise<string> {
        let params = {};

        let keycloakUrl = TestConstants.TS_SELENIUM_BASE_URL;
        const keycloakAuthSuffix = '/auth/realms/che/protocol/openid-connect/token';
        keycloakUrl = keycloakUrl.replace('che', 'keycloak') + keycloakAuthSuffix;
        params = {
            client_id: 'che-public',
            username: TestConstants.TS_SELENIUM_USERNAME,
            password: TestConstants.TS_SELENIUM_PASSWORD,
            grant_type: 'password'
        };
        
        try {
            const responseToObtainBearerToken = await axios.post(keycloakUrl, querystring.stringify(params));
            return responseToObtainBearerToken.data.access_token;
        } catch (err) {
            console.log(`Can not get bearer token. URL used: ${keycloakUrl}`);
            throw err;
        }

    }
}
