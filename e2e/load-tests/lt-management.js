import http from "k6/http";
import { check, fail } from "k6";
import { authenticate } from "./lib/auth-helper.js";
import { createStages } from "./lib/stage-setup.js";

const stages = createStages(700, 50, "30s", 5000);

export const options = {
  scenarios: {
    management: {
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

const kcTokenEndpoint = "https://idp-not.gxfs.dev/auth/realms/notarization-realm/protocol/openid-connect/token";
var notaryEndpoint = "https://notary-lt-not.gxfs.dev";

const kcGrantType = "password";

var kcClientID = "notary-client";
var kcClientSecret = "notary-client-secret";
var kcUsername = "notary-01";
var kcPassword = "notary-01-pw";

export function setup() {
  if (typeof __ENV.KC_CLIENT_ID != "undefined") {
    kcClientID = __ENV.KC_CLIENT_ID;
  } else {
    fail("Please provide a client secret to connect with the IdP (use env variable KC_CLIENT_ID)!");
  }

  if (typeof __ENV.KC_CLIENT_SECRET != "undefined") {
    kcClientSecret = __ENV.KC_CLIENT_SECRET;
  } else {
    fail("Please provide a client secret to connect with the IdP (use env variable KC_CLIENT_SECRET)!");
  }

  if (typeof __ENV.KC_USERNAME != "undefined") {
    kcUsername = __ENV.KC_USERNAME;
  } else {
    fail("Please provide a username to connect with the IdP (use env variable KC_USERNAME)!");
  }

  if (typeof __ENV.KC_PASSWORD != "undefined") {
    kcPassword = __ENV.KC_PASSWORD;
  } else {
    fail("Please provide a password to connect with the IdP (use env variable KC_PASSWORD)!");
  }

  if (typeof __ENV.NOTARY_BASE_URL != "undefined") {
    notaryEndpoint = __ENV.NOTARY_BASE_URL;
  }

  const authResp = authenticate(
    kcTokenEndpoint,
    kcGrantType,
    kcClientID,
    kcClientSecret,
    kcUsername,
    kcPassword
  );

  return authResp;
}

export default function (data) {
  const params = {
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${data.access_token}`,
    },
  };

  const url = `${notaryEndpoint}/api/v1/requests?offset=1&limit=5&filter=available`;

  const res = http.get(url, params);

  check(res, {
    "get requests status code is 200": (r) => r.status === 200,
  });

  if (res.status != 200) {
    console.log(res);
  }
}
