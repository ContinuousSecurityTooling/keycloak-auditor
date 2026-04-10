import fetch from 'node-fetch';
import { AuditedClientRepresentation, AuditedUserRepresentation } from './spi.js';

export interface Credentials {
  grantType: string;
  clientId: string;
  clientSecret: string;
}

export class AuditClient {
  private accessToken: string | undefined;
  private realmUrl: string;
  constructor(url: string, realm: string) {
    this.realmUrl = `${url}/realms/${realm}`;
  }
  async auth(credentials: Credentials): Promise<AuditClient> {
    const params = new URLSearchParams();
    params.append('client_id', credentials.clientId);
    params.append('client_secret', credentials.clientSecret);
    params.append('grant_type', credentials.grantType);
    params.append('scope', 'profile');
    const body = (await (
      await fetch(`${this.realmUrl}/protocol/openid-connect/token`, {
        method: 'POST',
        body: params,
      })
    ).json()) as Record<string, string>;
    this.accessToken = body['access_token'];
    return this;
  }

  public async userListing(): Promise<AuditedUserRepresentation[]> {
    const response = (await (
      await fetch(`${this.realmUrl}/auditing/users`, {
        headers: { Authorization: `Bearer ${this.accessToken}` },
      })
    ).json()) as Record<string, unknown>;
    if (response['error']) {
      throw new Error(
        `Please check your client config, did you enabled the access the API endpoint? Error: ${response['error']}`
      );
    }
    return response as unknown as AuditedUserRepresentation[];
  }

  public async clientListing(): Promise<AuditedClientRepresentation[]> {
    const response = (await (
      await fetch(`${this.realmUrl}/auditing/clients`, {
        headers: { Authorization: `Bearer ${this.accessToken}` },
      })
    ).json()) as Record<string, unknown>;
    if (response['error']) {
      throw new Error(
        `Please check your client config, did you enabled the access the API endpoint? Error: ${response['error']}`
      );
    }
    return response as unknown as AuditedClientRepresentation[];
  }
}
