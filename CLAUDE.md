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
    utils/                      # ConfigHelper, RuntimeHelper
  src/main/resources/META-INF/services/
    org.keycloak.events.EventListenerProviderFactory
    org.keycloak.services.resource.RealmResourceProviderFactory
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
- Attributes are: view=`["admin"]`, edit=`[]` (read-only, admin-visible only); set `KC_AUD_ALLOW_ADMIN_EDIT=true` to make edit=`["admin"]` so admin-side user updates carrying `aud_usr_last-login*` don't fail with `error-user-attribute-read-only`. `ensureAttribute()` reconciles the edit permission of already-registered attributes on re-registration.
- Both `registerForRealm()` and `registerClientLoginAttribute()` are **idempotent**
- Must set `session.getContext().setRealm(realm)` before calling `UserProfileProvider` in background tasks

## REST API (`AuditEndpoint`)

Base path: `/realms/{realm}/auditing/`

### OpenAPI spec

`AuditEndpoint` methods carry MicroProfile OpenAPI annotations (`@Operation`, `@APIResponse`,
`@Parameter`, plus class-level `@Tag` / `@Server` / `@SecurityScheme`). The
`smallrye-open-api-maven-plugin` runs at `process-classes` and writes
`spi/target/openapi/openapi.{yaml,json}`, which are then copied to `sdk/openapi.{yaml,json}`
(git-ignored) and bundled into the JAR under `META-INF/`. The class has **no class-level
`@Path`** (it's a Keycloak sub-resource); the scanner base path comes from the plugin's
`<scanResourceClasses>` mapping. `microprofile-openapi-api` is `provided` scope only — not
shipped in the fat-jar.

On release, `release.yml` also copies `spi/target/openapi/openapi.{yaml,json}` to
`keycloak-auditor-openapi.{yaml,json}` and uploads them as GitHub release assets.

Not currently attached as secondary Maven artifacts for Central deployment (e.g. via
`build-helper-maven-plugin` classifier `openapi`) — tried once and ruled out as a suspect during
the v2.4.1/v2.4.2 incident below, but removing it didn't fix the underlying issue, so it's just
left out rather than re-added without re-testing. If Central distribution of the spec is wanted,
prefer wrapping it in a `.jar` (a type Central's validator definitely recognizes) over raw
`.yaml`/`.json`, and test a real deployment before relying on it.

### 2026-10-04 incident: Central Portal deploy failing ("Bundle has content that does NOT have a .pom file")

`net.continuous-security-tools:keycloak-auditor` (root) + `keycloak-auditor-spi` releases
started failing at the `central-publishing-maven-plugin:0.11.0:publish` step for both v2.4.1 and
v2.4.2, with Central Portal rejecting the aggregated two-module bundle. The real cause: a
Renovate commit bumped `.mvn/wrapper/maven-wrapper.properties` from Maven **3.9.16 → 3.10.0** on
2026-10-03, the day before the first failure — Maven 3.10.0 switched to Resolver 2.x (a near
rewrite of the dependency/artifact engine), which `central-publishing-maven-plugin:0.11.0`
doesn't handle correctly for this reactor shape. Removing the openapi-spec Maven attachment
(the first suspect, see above) did **not** fix it, which is what isolated the Maven version as
the actual cause. Fix: pinned the wrapper back to 3.9.16 and added a Renovate `packageRule`
(`matchManagers: ["maven-wrapper"]`, `allowedVersions: "<3.10.0"`) so it won't silently re-bump.
Remove that rule once a `central-publishing-maven-plugin` release confirmed compatible with
Maven 3.10.x+ is in use.

| Method | Path | Auth required | Description |
|--------|------|--------------|-------------|
| GET | `users` | Bearer | JSON list of users with last-login |
| GET | `clients` | Bearer | JSON list of clients with last-login |
| GET | `users/csv` | Bearer | CSV download (Content-Disposition: attachment) |
| GET | `clients/csv` | Bearer | CSV download (Content-Disposition: attachment) |
| GET | `download` | **None** | HTML download page with JS-driven downloads |

**Important**: `authenticate()` is **not** called in the constructor — it is called inside `checkAccessRights()`. This allows the `/download` HTML page to be served without a Bearer token, while all data endpoints still enforce auth.

### Query parameters

`listUsers`, `listClients`, `downloadUsersCsv`, `downloadClientsCsv` accept:

- `?scope=current-realm` — always returns only the current realm (overrides master auto-scope)
- `?scope=all-realms` or omitted — returns all realms if `globalMasterAccess=true` OR token issuer is `"master"`
- `?realm=<name>` — when the caller has all-realm access, filters results to that specific realm only

The `/download` page shows a per-realm table: master realm gets an "All Realms" row + one row per realm (sorted); non-master realms get a single row. Each row has JSON/CSV download buttons that pass either `?realm=<name>` or `?scope=all-realms`.

## Admin console integration

**There is no admin sidebar entry.** `UiPageProviderFactory` was tried (again) and removed
(again): it only renders a generic list/detail CRUD form (an "Add item" page with plain
text/boolean/list fields) — there's no field type that renders as a clickable link, and the page
can't be positioned inside an existing admin console page (e.g. as a Realm Settings tab); it's
always its own standalone top-level nav item. It cannot deliver an actual link no matter how it's
configured.

**Access audit reports** directly via: `<keycloak-url>/realms/{realm}/auditing/download`

The only way to get a real clickable link inside the admin console would be forking/rebuilding
parts of Keycloak's own compiled admin-console frontend, or DOM-injecting a link via a custom
admin theme override script — both out of scope unless explicitly requested, since the latter is
fragile across Keycloak upgrades. Do not re-introduce `UiPageProviderFactory` for this purpose —
it has been tried twice and doesn't solve the problem.

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

## Build

```bash
mvn clean verify          # compile + all tests
mvn test -Dtest=SomeTest  # single test class
```

Final JAR: `spi/target/keycloak-auditor-spi.jar` (fat-jar with dependencies via assembly plugin).
TypeScript types are generated from model classes into `sdk/src/spi.ts` during `process-classes`.
The OpenAPI document is generated in the same phase into `spi/target/openapi/` and copied to `sdk/openapi.{yaml,json}`.
