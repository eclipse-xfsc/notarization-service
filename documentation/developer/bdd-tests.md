# BDD Tests

To demonstrate the functionality of the system, the project contains definitions and an environment to execute cucumber-tests for test-cases specified in Gherkin language.

Definitions of scenarios can be found in `e2e/cucumber-specs/src/test/resources/specs`.

Before running the system, make sure to define `QUARKUS_OIDC_CLIENT_CREDENTIALS_SECRET` in `deploy/local/docker-compose/services.yml`.
This secret is necessary to execute the TRAIN validation tests.
And in addition, you also have to configure it in `e2e/cucumber-specs/src/test/resources/application.properties`.

To execute the tests, first make sure the system is installed and running via Docker Compose as described in [install-docker-compose.md](install-docker-compose.md).

When the system is running the tests can be executed as follows:

```sh
./gradlew :e2e:cucumber-specs:acceptanceTest --rerun-tasks
```

One can limit the test execution by setting different tags in the following way:

```sh
CUCUMBER_FILTER_TAGS=@notary ./gradlew :e2e:cucumber-specs:acceptanceTest --rerun-tasks
```

This will execute only scenarios tagged with `@notary`.

This allows testing for different aspects of the system.
The test specification defines the following tags:

| Tag        | Description                                                                                                     |
|------------|-----------------------------------------------------------------------------------------------------------------|
| @system    | Tests showing the functionality of the system besides the use-cases of users, like logging, administration etc. |
| @requestor | Tests regarding use-cases of the requestor                                                                      |
| @notary    | Tests regarding use-cases of the notary                                                                         |
| @happypath | Tests showing the functionality of processes as specified                                                       |
| @security  | Tests that consider security relevant details, like authorization                                               |
| @base      | Tests showing the functionality of the base system                                                              |
| @ext1      | Tests showing the functionality of the Notarization API Extension 1                                             |
