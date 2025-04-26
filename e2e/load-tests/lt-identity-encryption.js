import http from "k6/http";
import { check, fail } from "k6";
import { authenticate } from "./lib/auth-helper.js";
import { createStages } from "./lib/stage-setup.js";

const sessionData = {
  profileId: "demo-vc-issuance-01-simple-with-eid-task", // Test Cluster
};

const kcTokenEndpoint =
  "https://idp-not.gxfs.dev/auth/realms/notarization-realm/protocol/openid-connect/token";
const kcGrantType = "password";
const kcClientID = "auto-notary-client";
const kcUsername = "auto-notary-user";

const stages = createStages(20, 20, "30s", 2000);

export const options = {
  scenarios: {
    identity_encryption: {
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

let kcClientSecret = __ENV.KC_CLIENT_SECRET;
let kcPassword = __ENV.KC_USER_PASSWORD;

export function setup() {
  // Check if the secrets are provided.
  if (kcClientSecret === undefined || kcPassword === undefined) {
    fail(
      "Both the client secret and user password must be provided via environment variables (KC_CLIENT_SECRET and KC_USER_PASSWORD)."
    );
  }
}

let baseurl = "https://request-lt-not.gxfs.dev";

export default function () {
  if (typeof __ENV.REQUEST_PROCESSING_HOSTNAME != "undefined") {
    baseurl = __ENV.REQUEST_PROCESSING_HOSTNAME;
  }

  // Create a session for a task with "eID-Validation" to get the sessionId and
  // a token.
  let url = baseurl + "/api/v1/session";

  let res = http.post(url, JSON.stringify(sessionData), {
    headers: { "Content-Type": "application/json" },
  });

  check(res, {
    "create session status code is 201": (r) => r.status === 201,
  });

  if (res.status != 201) {
    console.log("Unable to create session, status code: ", res.status);
  } else {
    fetchSession(res.json().sessionId, res.json().token);
  }
}

// Fetch the session data to get the taskId.
function fetchSession(sessionId, token) {
  let url = baseurl + "/api/v1/session/" + sessionId;

  let res = http.get(url, {
    headers: {
      "Content-Type": "application/json",
      token: token,
    },
  });

  check(res, {
    "fetch session status code is 200": (r) => r.status === 200,
  });

  if (res.status != 200) {
    console.log("Unable to fetch session status, status code: ", res.status);
  } else {
    startTask(sessionId, token, res.json().tasks[0].taskId);
  }
}

// Start the task for a given taskId, get the redirect URI and the nonce from
// the cookies.
function startTask(sessionId, token, taskId) {
  let url = baseurl + "/api/v1/session/" + sessionId + "/task?taskId=" + taskId;

  let res = http.post(url, JSON.stringify(""), {
    headers: {
      "Content-Type": "application/json",
      token: token,
    },
  });

  check(res, {
    "start task status code is 201": (r) => r.status === 201,
  });

  if (res.status != 201) {
    console.log("Unable to start identification task, status code: ", res.status);
  } else {
    beginIdentification(res);
  }
}

function beginIdentification(startTaskResp) {
  // Don't follow the redirect, because the bearer token and the cookie value
  // must be set explicitly in the next call.
  let res = http.get(startTaskResp.json().uri, {
    redirects: 0,
  });

  check(res, {
    "redirect status code is 303": (r) => r.status === 303,
  });

  if (res.status != 303) {
    console.log("Unable to begin identification, status code: ", res.status);
  } else {
    performIdentificationTask(res.headers.Location, res.cookies.nonce[0].value);
  }
}

// Follow the redirect from the last call. Pass the nonce as cookie value and
// the bearer token in the header.
function performIdentificationTask(location, cookieValue) {
  const access_token = getAccessToken();

  let res = http.get(location, {
    redirects: 0,
    cookies: {
      nonce: cookieValue,
    },
    headers: {
      Authorization: `Bearer ${access_token}`,
    },
  });

  check(res, {
    "identification task status code is 303": (r) => r.status === 303,
  });
}

function getAccessToken(sessionId, token, taskId) {
  const authResp = authenticate(
    kcTokenEndpoint,
    kcGrantType,
    kcClientID,
    kcClientSecret,
    kcUsername,
    kcPassword
  );

  return authResp.access_token;
}
