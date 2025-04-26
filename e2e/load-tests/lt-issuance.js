import http from "k6/http";
import { check } from "k6";
import { poll } from "./lib/poll.js";
import { createStages } from "./lib/stage-setup.js";

const defaultHeadersHolder = {
  headers: {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
};

const defaultHeadersIssuance = {
  headers: {
    "Accept": "application/json",
    "Content-Type": "application/json"
  },
};

const maxRetries = 15;
const stages = createStages(15, 10, "60s", 500);

export const options = {
  scenarios: {
    issuance: {
      executor: "ramping-vus",
      startVUs: 0,
      stages
    }
  },
  thresholds: {
    'http_req_duration': [
      {
        threshold: 'p(99)<2000',
        abortOnFail: true
      }
    ]
  }
};

// baseurl of the ACA-Py holder instance.
let baseurlAcapyHolder = "https://gaiax-4-api.acapy.spherity.io";
// for local docker-compose deployment
//let baseurlAcapyHolder = "http://localhost:30080"

// baseurl of the SSI Issuance Controller instance. Make sure the port is forwarded correctly:
// kubectl -n lt-not-api port-forward deployments/ssi-issuance-controller 8080:8080
let baseurlSsiIssuanceController = "http://localhost:8080";

// profile to use
let profile = "demo-vc-issuance-01-simple-with-eid-task";
// for local docker-compose deployment
// let profile = "demo-vc-issuance-01-simple";

let skipHolderCommunication = false;
let skipWaitingForCredential = false;

export default function () {
  if (typeof __ENV.BASEURL_ACAPY_HOLDER != "undefined") {
    baseurlAcapyHolder = __ENV.BASEURL_ACAPY_HOLDER;
  }

  if (typeof __ENV.BASEURL_SSI_ISSUANCE_CONTROLLER != "undefined") {
    baseurlSsiIssuanceController = __ENV.BASEURL_SSI_ISSUANCE_CONTROLLER;
  }

  // If the holder ACA-Py is protected via API key, you can set this API key via the env variable
  // ACAPY_HOLDER_API_KEY.
  if (typeof __ENV.ACAPY_HOLDER_API_KEY != "undefined") {
    defaultHeadersHolder.headers["X-API-KEY"] = __ENV.ACAPY_HOLDER_API_KEY;
  }

  if (typeof __ENV.SKIP_HOLDER_COMMUNICATION != "undefined") {
    skipHolderCommunication = __ENV.SKIP_HOLDER_COMMUNICATION;
  }

  if (typeof __ENV.SKIP_WAITING_FOR_CREDENTIAL != "undefined") {
    skipWaitingForCredential = __ENV.SKIP_WAITING_FOR_CREDENTIAL;
  }

  let holderDid = skipHolderCommunication ? "did:key:ads14" : createHolderDID();
  let invitation = skipHolderCommunication ? "https://invitation.holder.de" : createHolderInvitation(holderDid);

  sendCredential(holderDid, invitation);

  if (! skipHolderCommunication && ! skipWaitingForCredential) {
    getCredential(holderDid);
  }
}

function createHolderDID() {
  let res = http.post(
    baseurlAcapyHolder + "/wallet/did/create",
    JSON.stringify({
      method: "key",
      options: {
        key_type: "ed25519",
      }
    }),
    defaultHeadersHolder
  );

  check(res, {
    "create holder DID status code is 200": (r) => r.status === 200,
  });

  return res.json().result.did;
}

function createHolderInvitation(holderDID) {
  let res = http.post(
    baseurlAcapyHolder + "/connections/create-invitation?auto_accept=true",
    JSON.stringify({}),
    defaultHeadersHolder
  );

  check(res, {
    "create holder invitation status code is 200": (r) => r.status === 200,
  });

  return res.json();
}

function sendCredential(holderDID, invitationResponse) {
  let payload = {
    profileID: profile,
    holderDID: holderDID,
    invitationURL: invitationResponse.invitation_url,
    issuanceTimestamp: new Date().toISOString(),
    successURL: "http://request-processing:8084/success",
    failureURL: "http://request-processing:8084/fail",
    "credentialData": {
      "credentialSubject": {
        "givenName": "Jane",
        "familyName": "Doe",
        "gender": "Female",
        "image": "data:image/png;base64,iVBORw0KGgo...kJggg==",
        "residentSince": "2015-01-01",
        "lprCategory": "C09",
        "lprNumber": "999-999-999",
        "commuterClassification": "C1",
        "birthCountry": "Bahamas",
        "birthDate": "1958-07-17"
      }
    }
  };

  let res = http.post(
    baseurlSsiIssuanceController + "/credential/start-issuance/",
    JSON.stringify(payload),
    defaultHeadersIssuance
  );

  check(res, {
    "create credential send status code is 201": (r) => r.status === 201,
  });
}

function getCredential(holderDID) {
  // Use polling due to the async implementation of the acapy to make sure that
  // the results array is not empty.
  var statusCodeOK = false;
  poll(
    function () {
      let res = http.post(
        baseurlAcapyHolder + "/credentials/w3c",
        JSON.stringify({
          subject_ids: [holderDID],
        }),
        defaultHeadersHolder
      );

      if (res.status === 200 && res.json().results.length > 0) {
        statusCodeOK = true;
      }
    },
    function () {
      // Success callback
    },
    function () {
      // Failure callback
    },
    maxRetries
  );

  check(statusCodeOK, {
    "fetch credential was successful": (s) => s === true,
  });
}
