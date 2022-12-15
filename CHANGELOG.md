# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [1.0.14] - 2022-12-15
- Support access of Folder level crdentials to child folders & jobs.
-
## [1.0.13] - 2022-11-23
- Security updates in pom.xml & support to Java 11. The following depedency updates are made:
- org.jenkins-ci.plugins is updated from 4.17 to 4.48
- Jenkins version has been updated from 2.176.1 to 2.377
- kotlin-stdlib-common updated to 1.6.20
- okhttp has been updated from 3.11.0 to 4.10.0
- jackson-databind has been updated from 2.12.5 to 2.14.0
- gsom from 2.8.8 to 2.8.9
- io.jenkins.tools.bom artifact id updated from bom-2.164.x to bom-2.332.x

## [1.0.7] - 2021-10-05
- JWT token issuer is set to the root URL of the jenkins instance
- WebService ID for the authentication can be either the service id or authenticator_type/service_id (authn-jwt/id)
- Warning/error on validation for Key and Token TTL 

## [1.0.6] - 2021-09-27
- Updated README.md 
- Added "JWT Token Claims" button to configuration page to obtain referecence claims to be used by JWT Authenticator
- Fixed bindings for context aware store credentials

## [1.0.5] - 2021-09-23
- Added JWT Authentication
- Added Context Aware (Based on JWT) Credential Provider
- Updated Doc
- Misc fixes

## [1.0.4] - 2021-07-12
- Incorporated changes for null certificate on slave
- Brought fixes for core cyberark/conjur-credentials-plugin
- Release in plugins

##[ 1.0.2] - 2020-05-05

### Added
- Included changes to allow GIT plugin to retrieve credentials from slaves

### Removed
- Removed binaries deliverables, to use artifactory to deliver binaries

## 0.7.0 - 2019-09-27

### Added
- Added Support for SSH Private Key

[Unreleased]: https://github.com/cyberark/conjur-credentials-plugin/compare/v1.0.2...HEAD
[1.0.2]: https://github.com/cyberark/conjur-credentials-plugin/compare/v0.7.0...v1.0.2
