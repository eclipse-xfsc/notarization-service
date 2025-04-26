
- [Design Domain Driven](#design-domain-driven)
- [GDPR Concerns](#gdpr-concerns)
  - [GDPR Auditing](#gdpr-auditing)
  - [Additional GDPR-relevant data](#additional-gdpr-relevant-data)
- [Quality Aspects](#quality-aspects)

# Design Domain Driven

See the domain driven design [here](./ddd.md).

# GDPR Concerns

See [GDPR](./gdpr.md) for further details.

# Quality Aspects

Already during the design process, it was important to design services that are robust, reliable and scalable.
For this purpose, services are designed as stateless components which allows to start several replications of a service. The notarization system was structured as a collection of several small services that are loosely coupled and therefore individual maintainable and testable. 

During the development phase of the services, testing was an important factor. Tests were realized as JUnit or BDD-Tests.
Security-relevant tests were tagged with `@security`. Security concerns and possible mitigations are documented in the security concept.
