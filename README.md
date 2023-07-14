[![CI](https://github.com/ContinuousSecurityTooling/keycloak-auditor/actions/workflows/build.yml/badge.svg)](https://github.com/ContinuousSecurityTooling/keycloak-auditor/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ContinuousSecurityTooling_keycloak-auditor&metric=alert_status)](https://sonarcloud.io/dashboard?id=ContinuousSecurityTooling_keycloak-auditor)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ContinuousSecurityTooling_keycloak-auditor&metric=coverage)](https://sonarcloud.io/dashboard?id=ContinuousSecurityTooling_keycloak-auditor)
[![Known Vulnerabilities](https://snyk.io/test/github/ContinuousSecurityTooling/keycloak-auditor/badge.svg)](https://snyk.io/test/github/ContinuousSecurityTooling/keycloak-auditor)

# Keycloak Auditor

A Keycloak module to store audit data for user logins. The last logins will be saved as user attributes.

All events created by this listener follow a schema:

`aud_<resource-type>_<audit-event>(_<client>)`:`<TIME_STAMP>`

To retrieve the login data structure you can use a custom endpoint extensions via Keycloak Admin API:
```
curl http://localhost:8080/realms/master/auditing/users
```

![](.docs/example_user-auditing.png)

```
[
    {
        "id": "44c2cc1f-dd8e-4ca2-be61-21fe72305161",
        "createdTimestamp": 1687417311137,
        "username": "admin",
        "enabled": true,
        "emailVerified": false,
        "lastLogin": "2023-07-14T06:26:30.639007384Z",
        "clientLogins": {
            "security-admin-console": "2023-07-14T06:26:30.639007384Z"
        }
    },
    {
        "id": "7bfdd029-8dfd-49bb-abaa-09ab23dd6d3a",
        "createdTimestamp": 1687417490203,
        "username": "kermit",
        "enabled": true,
        "emailVerified": false,
        "firstName": "Kermit",
        "lastName": "the Frog",
        "email": "kermit@example.com",
        "lastLogin": "2023-07-14T06:26:56.97706909Z",
        "clientLogins": {
            "security-admin-console": "2023-07-14T06:26:44.97453346Z",
            "account-console": "2023-07-14T06:26:56.97706909Z"
        }
    }
]
```

>**NOTE**: 
> 
> The regular Keycloak ADMIN API Authentication is used.

## Setup

Prerequisites:
* JDK 11+
* Docker

Build and start:

```bash
# Build the extension
mvn clean package -DskipTests

# Start keycloak and MySQL database
docker-compose up -d
```

The Keycloak server will now be available on <http://localhost:8080>. You can log into the Administration Console using “**admin**” as both username and password.

## Deploy into a standalone keycloak server

>**NOTE**:
> Instead of building yourself, you can pick the latest release JAR.


1. Copy the built artifact from `deployment/target/auditor-module-for-keycloak.ear` into the directory `${keycloak.home}/standalone/deployments` of a keycloak server.  
**NOTE:** For Quarkus, just use the the jar at `./spi/target/keycloak-auditor-spi.jar`
2. Enable event listener:

![](.docs/keycloak-realm-event-config-step1.png)

![](.docs/keycloak-realm-event-config-step2.png)
