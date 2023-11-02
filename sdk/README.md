# Keycloak Auditor SDK

Typings for [Keycloak Auditor](https://github.com/ContinuousSecurityTooling/keycloak-auditor) SPI Rest Endpoint:

You can also the NPM package `@continuoussecuritytooling/keycloak-auditor` to directly use the rest endpoint:

```
import { AuditClient } from '@continuoussecuritytooling/keycloak-auditor';

const keycloakUrl = '';
const clientId = '';
const clientSecret = '';

const kcClient = new AuditClient(keycloakUrl, 'master');
// client login
await kcClient.auth({
    clientId: clientId,
    clientSecret: clientSecret,
    grantType: 'client_credentials',
});
const users = await client.userListing();
const clients = await client.clientListing();
```