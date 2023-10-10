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
