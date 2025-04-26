# TRAIN Integration

The notarization API supports the integration of TRAIN for enrolling new issuers and authorities but also for validating and verifying verifiable presentations.

# Enrollment of new issuers and authorities 

The TRAIN-Service can be integrated to make it possible to enroll new issuers and authorities.
For this purpose, the `post-issuance-action` `TRAIN-Enrollment` can be used.
An example profile can look like the following:

```yaml
name: Train Example Profile
aip: "2.0"
id: demo-train-enrollment
description: Simple profile to perform a train enrollment.
notaries:
- jwk: >
    {
        "kty": "EC",
        "use": "enc",
        "crv": "P-384",
        "kid": "CJVbr_T_rbgAWkP3TBqqbgTO2w27ZWfE_Gsky88odao",
        "x": "s9zwNWhaFk1S_Pm4Ec05wztC5iZ6W1QPemcywckgKk2fXx8IBFC9vTc2x0LatSTC",
        "y": "c9xoL-nBZmifO42GCWnU32uksdg1TF-YmldqxVf_apJ6Yi-bG_cAu0LP3yAqXijw",
        "alg": "ECDH-ES+A256KW"
    }
valid-for: P100Y
is-revocable: false
task-descriptions: >
  [ ]
action-descriptions: >
    [
        {
            "name": "TRAIN-Enrollment",
            "description": "Performing an enrollment with TRAIN."
        }
    ]
tasks: >
    { }
precondition-tasks: >
    { }
post-issuance-actions:
  - "TRAIN-Enrollment"
document-template: >
    { }
template: >
    {
        "@context": [
            "https://www.w3.org/2018/credentials/v1",
            "https://w3id.org/citizenship/v1"
        ],
        "type": ["VerifiableCredential", "PermanentResidentCard"],
        "credentialSubject": {
            "type": "PermanentResident"
        }
    }
```

This above example shows the configuration of a post-issuance action (that is, an action performed by the notary operator after approving the notarization request.). In breif, after issuance, the notarization operator performs the TRAIN enrollment, with a notarization API specific service acting as proxy.

When deploying the `request-processing` service the following environment variables must be set:

```
GAIA_X_EXTENSIONS_ACTIONS_TRAINENROLLMENT_SERVICE_NAME: TRAIN-Enrollment
GAIA_X_EXTENSIONS_ACTIONS_TRAINENROLLMENT_NAMES: TRAIN-Enrollment
GAIA_X_EXTENSIONS_ACTIONS_TRAINENROLLMENT_LOCATION: http://train-enrollment:8092/task/begin
```

Thereby, the `GAIA_X_EXTENSIONS_ACTIONS_TRAINENROLLMENT_LOCATION` environment variable points to the `Extension-Service-API` of the provided `train-enrollment` service.

In the case above, a notarization operator has to fetch the post-issuance actions after accepting a notarization request.
Such a request can look like the following:

```
Request method:	GET
Request URI: https://<REQUEST_PROCESSING_HOST>/api/v1/profiles/demo-train-enrollment/requests/52477529-78b4-46d0-9aa1-3f690e55e390/actions
Headers:        Authorization=Bearer ...
                Accept=*/*
                Content-Type=application/json
HTTP/1.1 200 OK
content-length: 160
Content-Type: application/json;charset=UTF-8
[
    {
        "taskId": "715af5fc-7cee-4989-ba3d-620bda119c91",
        "uri": "http://localhost:8092/task/FJ4PWMIo4nY81oGcWAi_4nqKVA7yN3Z8/enrollment",
        "taskName": "TRAIN-Enrollment"
    }
]
```

The `TRAIN-Enrollment` action contains a URL to which the enrollment data can be posted. This train enrollment will be propagated to the respective TRAIN framework list and must conform to the schema expected by TRAIN. Such as:

```json
{
  "TrustServiceProvider":{
      "UUID": "1-company-a",
	  "TSPName": "Company A Gmbh",
	  "TSPTradeName": "Company A Gmbh",
	  "TSPInformation": {
		"Address": {
		  "ElectronicAddress": "info@companya.de",
		  "PostalAddress": {
			"City": "Stuttgart",
			"Country": "DE",
			"PostalCode": "11111",
			"State": "BW",
			"StreetAddress1": "Hauptstr",
			"StreetAddress2": "071"
		  }
		},
		"TSPCertificationList":{
		"TSPCertification": [
		  {
			"Type": "ISO:9001",
			"Value": "4356546745"
		  },
		  {
			"Type": "EU-VAT",
			"Value": "4356546745"
		  }
		]
		},
		"TSPEntityIdentifierList": {
		  "TSPEntityIdendifier":[ {
			"Type": "vLEI",
			"Value": "3453654764"
		  },
		  {
			"Type": "VAT",
			"Value": "3453654764"
		  }
		  ]
		},
		"TSPInformationURI": "string"
	  },
	  "TSPServices": {
		"TSPService": [{
		  "ServiceName": "Federation Notary",
		  "ServiceTypeIdentifier": "string",
		  "ServiceCurrentStatus": "string",
		  "StatusStartingTime": "string",
		  "ServiceDefinitionURI": "string",
		  "ServiceDigitalIdentity": {
			"DigitalId":{
			  "X509Certificate": "sgdhfgsfhdsgfhsgfs",
			  "DID": "did:web:essif.iao.fraunhofer.de"
			}
		  },
		  "AdditionalServiceInformation": {
			"ServiceBusinessRulesURI": "string",
			"ServiceGovernanceURI": "string",
			"ServiceIssuedCredentialTypes":{
				"CredentialType": [
			  {
				"Type": "string"
			  },
			  {
				"Type": "string"
			  }
			]
			},
			"ServiceContractType": "string",
			"ServicePolicySet": "string",
			"ServiceSchemaURI": "string",
			"ServiceSupplyPoint": "string"
		  }
		}
	  ]}
	}
}							
```

For more information about the enrollment, schema or values of issuers and authorities in TRAIN, refer to the [
Train Trust Framework Manager](https://gitlab.eclipse.org/eclipse/xfsc/train/tspa) and the [Train Architecture Documentation](https://gitlab.eclipse.org/eclipse/xfsc/train/TRAIN-Documentation).

## Validation and Verification via TRAIN

The `oid4vp` service can be extended by doing an additional validation and verification via TRAIN.
For this purpose, only an additional environment variable must be configured:

```
QUARKUS_REST_CLIENT_TRAIN_API_URL: https://tcr.train.xfsc.dev/tcr/v1
```

This environment variable is optional and if it is not provided, no TRAIN validation will be executed.
More information about the validation and verification via TRAIN can be found at the [TRAIN repository](https://gitlab.eclipse.org/eclipse/xfsc/train/trusted-content-resolver).
