# Request submission postman example

The minimum calls to submit a request and then fetch the invitation link are captured in this file [./not-api-minimal.postman_collection.json](./not-api-minimal.postman_collection.json) as a [postman collection](https://www.postman.com/).

To test this against the demo-staging deployment, load and apply the postman environment file: [not-api-test.postman_environment.json](not-api-test.postman_environment.json)

Step 0 fetches the configured profiles, which each define a notarization process that leads to an expected type of VC to be issued, as configured by the administrator of the Notarization System must define the profile.

The two step 4 operations are pollable endpoints. They can be polled until the request is in the accepted state, at which point an invitation URL is available.

To enter the accepted state, the notarization request must be accepted by a notary.

# Comprehensive postman example

A comprehensive example of API calls relevant for the requestor front end and the notary front end can be found in the file [./comprehensive-not-api.postman_collection](./comprehensive-not-api.postman_collection) as a [postman collection](https://www.postman.com/). These calls are not exhaustive.

A usage guide is not available, because this API is equivalent to two front-end implementations.

# Extension 1 postman example

A comprehensive example of API calls relevant for the extension-1 of the notarization can be found in the file [./ext.1-not-api.postman_collection.json](./ext.1-not-api.postman_collection.json) as a [postman collection](https://www.postman.com/).
