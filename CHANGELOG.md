## [1.1.2](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v1.1.1...v1.1.2) (2023-11-28)


### Bug Fixes

* **deps:** update dependency org.jodd:jodd-util to v6.2.2 ([82fe39b](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/82fe39be086faee247ac408eec9830ccabb1dda9))
* **deps:** update dependency org.keycloak:keycloak-parent to v23 ([9d886bf](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/9d886bf091622681ad1438856774ce57769aa263))



## [1.1.1](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v1.1.0...v1.1.1) (2023-11-02)


### Bug Fixes

* Correcting tsc error ([38ff8a9](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/38ff8a99f9361e264789920a7137679835219bf5))
* **SDK:** Improve error handling ([0cc2b2c](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/0cc2b2c661a8c75f78ef53e33696c0907c5822d9))
* **Security:** Resolve security finding in babel ([fa682f4](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/fa682f4feb00f6c400dd1f795e9f46784d308363))



# [1.1.0](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v1.0.2...v1.1.0) (2023-10-27)


### Features

* **SDK:** Adding NodeJS rest client ([d4fa162](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/d4fa162e9e05c9f83b83a2442d3af15a54d63204))



## [1.0.2](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v1.0.1...v1.0.2) (2023-10-26)


### Bug Fixes

* **deps:** update dependency org.keycloak:keycloak-parent to v22.0.5 ([7500c83](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/7500c83ffbdd3d3e6e671045e572333ecadf31fa))



## [1.0.1](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v1.0.0...v1.0.1) (2023-10-16)


### Bug Fixes

* **Release:** Correcting release packaging ([cb1ee68](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/cb1ee6844c851c5fbebc2d37d8597a34288c2238))



# [1.0.0](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v0.5.0...v1.0.0) (2023-10-14)


### Bug Fixes

* Correcting ts generator output ([4bbf8b9](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/4bbf8b9ebeff7dc81f4929f578751025afe45c12))


### Features

* **Master Access:** Allow fetching of all users/clients from master realm ([a344096](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/a3440961c5a5241eee7807fec436758df08d6ff6))
* **SDK:** Fix SDK setup ([70481a6](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/70481a6da40877dcc1c09c6d3a5d6017e83fa82c))


### BREAKING CHANGES

* **Master Access:** To enable this feature set env var `KC_AUD_GLOBAL_MASTER_ACCESS` to `true`



## [0.5.1-SNAPSHOT](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v0.5.0...v0.5.1-SNAPSHOT) (2023-10-14)


### Bug Fixes

* Correcting ts generator output ([4bbf8b9](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/4bbf8b9ebeff7dc81f4929f578751025afe45c12))


### Features

* **Master Access:** Allow fetching of all users/clients from master realm ([a344096](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/a3440961c5a5241eee7807fec436758df08d6ff6))
* **SDK:** Fix SDK setup ([70481a6](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/70481a6da40877dcc1c09c6d3a5d6017e83fa82c))


### BREAKING CHANGES

* **Master Access:** To enable this feature set env var `KC_AUD_GLOBAL_MASTER_ACCESS` to `true`



# [0.5.0](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v0.4.1...v0.5.0) (2023-10-10)


### Bug Fixes

* **deps:** update dependency org.keycloak:keycloak-parent to v22.0.4 ([ab805c2](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/ab805c281994ba6f129650627b09649f4dc2a47a))
* **deps:** update dependency org.projectlombok:lombok to v1.18.30 ([752c344](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/752c344112b89d7e2c5c6f83f8fba8077f271237))
* **deps:** update dependency org.slf4j:slf4j-api to v2.0.9 ([af3065d](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/af3065d9f69616af1c906fdf73ef1e4419907cc5))


### Features

* **Config:** Adding config via var env ([d03e931](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/d03e931c132294e01c1e1b07d8824a4409a364ca)), closes [#46](https://github.com/ContinuousSecurityTooling/keycloak-auditor/issues/46)
* **Config:** Finalize configuration options ([8d28232](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/8d282325a90be5613e12a2ed3019dbd99c8a659c)), closes [#46](https://github.com/ContinuousSecurityTooling/keycloak-auditor/issues/46)
* **SDK:** Update npm typing SDK ([67a0c0a](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/67a0c0a462d28ef991135265f3ffb0b9e2625410))



## [0.4.1](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v0.4.0...v0.4.1) (2023-10-04)


### Features

* **Testing:** Adding end2end testing ([0c79def](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/0c79defdebc02e6610a0782d10df8c9ce551efba))



# [0.4.0](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v0.3.1...v0.4.0) (2023-08-09)


### Features

* **Keycloak:** Make SPI working with KC 22 ([a176bb3](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/a176bb3af51c241bd58146651b3ef9084b59294c))



## [0.3.1](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v0.3.0...v0.3.1) (2023-08-09)


### Bug Fixes

* **Keycloak:** Make EAR working in Keycloak Legacy ([c73fb0c](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/c73fb0c9e2008230ed3003842ca631eee680c437))


### Features

* **Keycloak:** Update Keycloak version for dev-setup ([db640ac](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/db640ac4c994e7d0620223a0dee2ddacbc2af76a))



# [0.3.0](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/v0.2.1...v0.3.0) (2023-08-09)


### Bug Fixes

* **deps:** update dependency org.slf4j:slf4j-api to v2 ([338aaba](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/338aaba2c84b1635112edad269235fd5fb9d9000))


### Features

* Use flat user attributes and aggregate via custom rest endpoint ([2618843](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/2618843aeb909113b4892988651384116f4065c5))



## [0.2.1](https://github.com/ContinuousSecurityTooling/keycloak-auditor/compare/fceae7576defe53fa529a824ddb44c7c3855fde2...v0.2.1) (2023-07-03)


### Bug Fixes

* **Config:** Renaming listener ([ac46c74](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/ac46c74dac9e7486b198d87c938f99e01b810115))
* **deps:** update dependency org.bouncycastle:bcprov-jdk15on to v1.70 ([74fc359](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/74fc359f55991c312b325771c51bad9ce8f2f78e))
* **deps:** update dependency org.keycloak:keycloak-parent to v20 [security] ([eac0f9b](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/eac0f9b299850cbaf188e11b9bf245a9acb24297))
* **deps:** update dependency org.keycloak:keycloak-parent to v21 ([214bfdc](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/214bfdc29ffa5ef22eaf779410d7696c6729ea37))
* **deps:** update dependency org.keycloak:keycloak-parent to v21.1.2 ([ce3513b](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/ce3513b979aeeed8a7bf1c4c0a260c2aed885c49))
* **deps:** update dependency org.projectlombok:lombok to v1.18.28 ([851aa09](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/851aa0925270c87e02529ee7912fe3a550e3b58f))
* **NPM:** Adding npm package info ([dcde5bc](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/dcde5bccfc5e771a2a2ec709a1453a8a7ba83d39))


### Features

* Adding typescript export of constans ([435fe4a](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/435fe4ae2d94e865841bc9ae11cf4b77e694f556))
* **API:** Adding typings npm package ([4e8ade2](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/4e8ade289f0925c176bd4c4487cbce6764800139))
* Initial release ([fceae75](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/fceae7576defe53fa529a824ddb44c7c3855fde2))
* **Storage:** Use JSON structure for last login data ([42f1286](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/42f1286fd88997bf39220ed982ab120afd461865))
* Use Java 11 by default ([c3bce2f](https://github.com/ContinuousSecurityTooling/keycloak-auditor/commit/c3bce2f01704bd5489f3eea55074403e9f5d80cf))


### BREAKING CHANGES

* **Storage:** This structural change is not backwards compatible
