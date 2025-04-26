# Test Strategy for Notarization API

This document describes the test strategy for the project [Notarization API Service](https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not)

## 1. Introduction
### Purpose of Testing

- Ensure the Notarization API is reliable, secure, and integrates smoothly with external systems.
- Validate tamper-proof digital credential generation.

### Scope
- Focus on core functionalities, integration capabilities, and non-functional requirements like performance and scalability.

---

## 2. Objectives
- Validate the transformation of master data into verifiable credentials.
- Ensure tamper-proof digital claims maintain integrity and trustworthiness.
- Verify compatibility and ease of integration with external systems.
- Guarantee a user-friendly experience for non-IT operators.

---

## 3. Test Types
- **Unit Testing**: Test individual modules, especially notarization logic and credential transformation algorithms. These tests are executed by developers, and by the CI pipeline.
  - **Tools**: JUnit, Quarkus Mocks.
- **Integration Testing**: Validate API interactions within sub-projects and with external services. These tests are executed by developers, and by the CI pipeline.
  - **Tools**: WireMock, Quarkus Dev Services.
- **Requirements Testing**: Test high-level requirements and interactions between services. These tests are executed by developers.
  - **Tools**: Cucumber behavior tests, WireMock, Docker Compose.
- **Performance Testing**: Assess API response time and throughput under varying load conditions.  These tests are executed by developers.
  - Conducted in the load test environment on a Kubernetes cluster.
- **Regression Testing**: Ensure new changes do not break existing functionality. This is covered by the tests executed by the CI pipeline.
- **Acceptance Testing**: Verify integration with third-party wallets in the acceptance test environment. These tests are executed by developers.

---

## 4. Test Environments
- **Development Test Environment**:
  - **Purpose**: Unit and integration tests for each sub-project.
  - **Tools**: JUnit, WireMock, Quarkus Mocks, Quarkus Dev Services, and Docker containers.
- **Local System Test Environment**:
  - **Purpose**: System tests and testing high-level requirements.
  - **Setup**: Developers use Docker Compose to start all services and third-party services locally.
  - **Tools**: Cucumber behavior tests, WireMock, and Docker.
- **Load Test Environment**:
  - **Purpose**: Performance and load testing.
  - **Setup**: Deployment on a Kubernetes cluster.
- **Acceptance Test Environment**:
  - **Purpose**: Verify integration with third-party wallets.
  - **Setup**: Separate deployment on a Kubernetes cluster.

---

## 5. Test Data
- Use anonymized or synthetic master data that covers diverse use cases.
- Include edge cases for uncommon attributes or formats.
- where possible, maintain libraries of functionality to generate the data sets.

---

## 6. Test Execution Process
- The CI system automatically executes all service-specific tests during development.
- Pre-release activities include:
  - Executing Cucumber tests to validate high-level system behaviors.
  - Conducting load tests to verify performance under expected and extreme conditions.
  - Performing acceptance tests to confirm proper integration with third-party wallets.
- Separate test management tools are not required, as the GitLab repository issue tracker manages all testing and defect tracking.

---

## 7. Reporting and Metrics
- **Key Metrics**:
  - Number of tests and test execution time.
    See the test jobs in the pipeline for the current tests: https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not/-/pipelines/49278/test_report?job_name=test-services
  - Test coverage (unit, integration, and system tests).
  - Number and severity of defects.
- **Reporting Frequency**:
  - Regular updates during development cycles.
  - Comprehensive summary reports post-release.

---

## 8. Risk Assessment
- **Potential Risks**:
  - Misalignment with external systems' requirements.
- **Mitigation Strategies**:
  - Continuous collaboration in tickets with stakeholders for requirement validation.
  - Load testing in pre-production environments.

---

## 9. Tools and Technologies
- **Unit Testing**: JUnit, Quarkus Mocks, Mockito.
- **Integration Testing**: WireMock, Quarkus Dev Services.
- **System Testing**: Cucumber, JUnit, Docker Compose.
- **Requirements Testing**: k8s, Kubernetes.
- **Performance Testing**: k8s, Kubernetes.

---

## 10. Bug and Defect Management
- Bugs and defects are logged, tracked, and managed within the issue tracker of the GitLab repository. Bug tickets are tagged with the tag [`bug`](https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not/-/issues/?sort=created_date&state=opened&label_name%5B%5D=bug&first_page_size=20).

---

## 11. Conclusion
- The Notarization API test strategy ensures a secure, robust, and user-friendly API.
- Automated continuous testing and testing improvements are prioritized to meet the needs of all stakeholders.
