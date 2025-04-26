# Decision Engine Integration

It is possible to automatically make decisions for issuing based on OAW and TSA policy outcomes.
For this purpose, the [TSA-Policy Service](https://gitlab.eclipse.org/eclipse/xfsc/tsa/policy/) can be used.
The notarization API provides endpoints to claim and accept requests.
Requests can be automatically accepted when a TSA policy outcome returns an appropriate result.
This behavior was simulated in a BDD-Test (CP.NOTAR.E1.00064.DecisionEngine.feature).
For more information about the TSA-Policy Service take a look [here](https://gitlab.eclipse.org/eclipse/xfsc/tsa/policy/).
