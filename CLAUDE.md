# keycloak-auditor — CLAUDE.md

## Project overview

A Keycloak SPI plugin that:
- Listens for LOGIN / CLIENT_LOGIN events and writes last-login timestamps as user/client attributes
- Exposes a REST API (`/realms/{realm}/auditing/`) for querying audit data
- Registers Declarative User Profile attribute definitions so timestamps appear in the admin console
- Provides an HTML download page and CSV exports for audit reports

## Module structure

```
spi/                            # only Maven module
  src/main/java/net/cst/keycloak/
    audit/model/                # domain models + constants
    events/logging/             # LoginEventListenerProvider + Factory
    resources/                  # AuditEndpoint (JAX-RS) + AuditedResourcesProvider/Factory
    userprofile/                # AuditUserProfileRegistrar
    admin/                      # AdminUiPage (UiPageProvider)
    utils/                      # ConfigHelper, RuntimeHelper
  src/main/resources/META-INF/services/
    org.keycloak.events.EventListenerProviderFactory
    org.keycloak.services.resource.RealmResourceProviderFactory
    org.keycloak.services.ui.extend.UiPageProviderFactory
```

## Keycloak version

**26.5.4** — key package facts for this version:

| What | Package / class |
|------|----------------|
| User profile config model | `org.keycloak.representations.userprofile.config.UPConfig` (in `keycloak-core`) |
| Attribute definition | `org.keycloak.representations.userprofile.config.UPAttribute` |
| Permissions | `org.keycloak.representations.userprofile.config.UPAttributePermissions` |
| Groups | `org.keycloak.representations.userprofile.config.UPGroup` |
| Runtime provider | `org.keycloak.userprofile.UserProfileProvider` (in `keycloak-server-spi-private`) |
| Realm iteration helper | `org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction()` |

**Critical**: `UPConfig.getAttributes()` returns **null** on a fresh instance — always null-check and initialise with `new ArrayList<>()` before calling `.add()`. `UPConfig.addGroup()` helper exists, but `addAttribute()` does **not**.

## User attribute naming

Stored as plain KC user attributes:
- `aud_usr_last-login` — global last login timestamp (ISO-8601)
- `aud_usr_last-login_{clientId}` — per-client last login timestamp
- `aud_cls_last-login` — last login on a client object

Constants are in `net.cst.keycloak.audit.model.Constants` enum.

## Declarative User Profile registration (`AuditUserProfileRegistrar`)

- Called from `LoginEventListenerProviderFactory.postInit()` for all existing realms at startup
- Called from `LoginEventListenerProvider.onEvent()` for new per-client attributes on first login
- Creates an `"audit"` attribute group with header "Audit Information"
- Attributes are: view=`["admin"]`, edit=`[]` (read-only, admin-visible only)
- Both `registerForRealm()` and `registerClientLoginAttribute()` are **idempotent**
- Must set `session.getContext().setRealm(realm)` before calling `UserProfileProvider` in background tasks

## REST API (`AuditEndpoint`)

Base path: `/realms/{realm}/auditing/`

| Method | Path | Auth required | Description |
|--------|------|--------------|-------------|
| GET | `users` | Bearer | JSON list of users with last-login |
| GET | `clients` | Bearer | JSON list of clients with last-login |
| GET | `users/csv` | Bearer | CSV download (Content-Disposition: attachment) |
| GET | `clients/csv` | Bearer | CSV download (Content-Disposition: attachment) |
| GET | `download` | **None** | HTML download page with JS-driven downloads |

**Important**: `authenticate()` is **not** called in the constructor — it is called inside `checkAccessRights()`. This allows the `/download` HTML page to be served without a Bearer token, while all data endpoints still enforce auth.

### Master realm auto-scope

When the access token issuer realm is `"master"` (i.e. `auth.getIssuer()` ends with `/master` or equals `"master"`), both `listUsers` and `listClients` automatically include **all realms** — regardless of the `KC_AUD_GLOBAL_MASTER_ACCESS` flag. Other realms only see their own data unless `KC_AUD_GLOBAL_MASTER_ACCESS=true` is set.

The `/download` HTML page also shows a realm-aware scope label: "Downloads include all realms." for master, or "Downloads include realm: {name}." for others, using `keycloakSession.getContext().getRealm()`.

## Admin console integration (`AdminUiPage`)

- Implements `UiPageProvider` + `UiPageProviderFactory` — appears in KC admin sidebar as "Auditing"
- ID must be `"auditing"` (matches `AuditedResourcesProviderFactory.CONTEXT_PATH`)
- `getTypeMetadata()` exports `displayName` ("Auditing"), `downloadPage`, `usersEndpoint`, `usersCsvEndpoint`, `clientsEndpoint`, `clientsCsvEndpoint` keys
- **`UiPageProviderFactory` / `ComponentFactory` does NOT have `getDisplayType()`** — display name must be in `getTypeMetadata()` as `"displayName"`
- The HTML download page is at `<realm-base-url>/auditing/download`

## Test patterns

All REST endpoint tests extend `net.cst.keycloak.utils.EndpointTest`.

Key mocking idiom:
```java
try (MockedStatic<Tokens> tokenMock = mockStatic(Tokens.class)) {
    tokenMock.when(() -> Tokens.getAccessToken(session)).thenReturn(token);
    AuditEndpoint endpoint = new AuditEndpoint(session) {
        @Override public void authenticate() { /* no-op */ }
    };
    // call endpoint methods...
}
```

`EndpointTest` provides `getUsersViaEndpoint()` / `getClientsViaEndpoint()` (default issuer "master") and overloaded variants `getUsersViaEndpoint("other")` to test non-master realm behavior.

For `LoginEventListenerProviderTest`, mock `UserProfileProvider` in `@BeforeAll`:
```java
UserProfileProvider upp = mock(UserProfileProvider.class);
when(session.getProvider(UserProfileProvider.class)).thenReturn(upp);
when(upp.getConfiguration()).thenReturn(new UPConfig());
```

Test classes by concern:
- `AuditEndpointTest` — `toBriefRepresentation()` static helpers
- `AuditEndpointDownloadTest` — CSV + HTML download endpoints; note: download page test must mock `session.getContext().getRealm()` (NPE otherwise)
- `AuditEndpointAccessRightsTest` — auth/role enforcement
- `AuditEndpointWithMasterAccessTest` — `KC_AUD_GLOBAL_MASTER_ACCESS=true`, expects 4 records
- `AuditEndpointWithoutMasterAccessTest` — uses issuer "other" (non-master), `KC_AUD_GLOBAL_MASTER_ACCESS=false`, expects 2 records
- `AuditEndpointMasterRealmAutoAccessTest` — issuer "master", `KC_AUD_GLOBAL_MASTER_ACCESS=false`, still expects 4 records (auto-scope)
- `AuditUserProfileRegistrarTest` — idempotency, permissions, group assignment
- `AdminUiPageTest` — metadata keys, config properties

## Build

```bash
mvn clean verify          # compile + all tests
mvn test -Dtest=SomeTest  # single test class
```

Final JAR: `spi/target/keycloak-auditor-spi.jar` (fat-jar with dependencies via assembly plugin).
TypeScript types are generated from model classes into `sdk/src/spi.ts` during `process-classes`.
