[![CI](https://github.com/ContinuousSecurityTooling/keycloak-auditor/actions/workflows/build.yml/badge.svg)](https://github.com/ContinuousSecurityTooling/keycloak-auditor/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ContinuousSecurityTooling_keycloak-auditor&metric=alert_status)](https://sonarcloud.io/dashboard?id=ContinuousSecurityTooling_keycloak-auditor)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=ContinuousSecurityTooling_keycloak-auditor&metric=coverage)](https://sonarcloud.io/dashboard?id=ContinuousSecurityTooling_keycloak-auditor)
[![Known Vulnerabilities](https://snyk.io/test/github/ContinuousSecurityTooling/keycloak-auditor/badge.svg)](https://snyk.io/test/github/ContinuousSecurityTooling/keycloak-auditor)

# Keycloak Auditor

A Keycloak module to store audit data for user logins as attributes:

![](.docs/example_user-auditing.png)

The last logins will be saved as JSON as user attribute:

```
{
  "kcLogin": "2023-06-29T10:52:40.485088378Z",
  "clientLogins": {
    "account-console": "2023-06-29T10:52:40.485088378Z",
  "account-console": "2023-06-29T10:58:05.075059792Z"
  }
}
```

All events created by this listener follow a schema:

`aud_<resource-type>_<audit-event>`:`<JSON_STRING>`

Audit events for user append also the client id of the used client.

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

1. Copy the built artifact from `deployment/target/auditor-module-for-keycloak.ear` into the directory `${keycloak.home}/standalone/deployments` of a keycloak server.  
**NOTE:** For Quarkus, just use the the jar at `./spi/target/keycloak-auditor-spi.jar`
2. Enable event listener:

![](.docs/keycloak-realm-event-config-step1.png)

![](.docs/keycloak-realm-event-config-step2.png)
