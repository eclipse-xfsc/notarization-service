# GAIA-X Notarization API SSI Issuance

## Getting started

### Prerequisites

* Node.js 16+
* npm

### How to start

1. Bootstrap the project:

    ```bash
    $ cd services/ssi-issuance
    $ npm install
    ```

2. Start the application:

    ```bash
    $ npm run start:debug
    ```

### Order of execution

1. NestJS executes `main` file when `start` command is executed. In this file the Open Telemetry collector is initialized if enabled
2. `main` file executes the `bootstrap` file once done with the Open Telemetry module.
3. `boostrap` creates a NestJS application using the module exported from `application` file.

### Customizing application settings

TBD