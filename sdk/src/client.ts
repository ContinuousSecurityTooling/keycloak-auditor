import fetch from 'node-fetch';
import { AuditedClientRepresentation, AuditedUserRepresentation, ClientLoginDetails, UserLoginDetails } from './spi.js';

export interface Credentials {
  grantType: string;
  clientId: string;
  clientSecret: string;
}

export class AuditClient {
  private accessToken: string;
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
    this.accessToken = (
      await (
        await fetch(`${this.realmUrl}/protocol/openid-connect/token`, {
          method: 'POST',
          body: params,
        })
      ).json()
    )['access_token'];
    return this;
  }

  public async userListing(): Promise<AuditedUserRepresentation[]> {
    const users: Array<AuditedUserRepresentation> = <Array<AuditedUserRepresentation>>(<unknown>(
      await fetch(`${this.realmUrl}/auditing/users`, {
        headers: { Authorization: `Bearer ${this.accessToken}` },
      })
    ).json());
    return users;
  }

  public async clientListing(): Promise<AuditedClientRepresentation[]> {
    const clients: Array<AuditedClientRepresentation> = <Array<AuditedClientRepresentation>>await (
      await fetch(`${this.realmUrl}/auditing/clients`, {
        headers: { Authorization: `Bearer ${this.accessToken}` },
      })
    ).json();
    return clients;
  }
}
