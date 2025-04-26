import http from "k6/http";
import { check } from "k6";
import { createStages } from "./lib/stage-setup.js";

const sessionData = {
  profileId: "demo-vc-issuance-01-simple-without-tasks", // Test Cluster
};
const submissionData = JSON.parse(open("./submission-payload.json"));
const stages = createStages(50, 20, "30s", 5000);                                                                 

export const options = {
  scenarios: {
    submission: {
      executor: "ramping-vus",
      startVUs: 0,
      stages
    },
  },
  thresholds: {
    'iteration_duration': [
      {
        threshold: 'p(99)<5000',
        abortOnFail: true
      }
    ]
  }
};

let baseurl = "https://request-lt-not.gxfs.dev";

export default function () {
  if (typeof __ENV.REQUEST_PROCESSING_HOSTNAME != "undefined") {
    baseurl = __ENV.REQUEST_PROCESSING_HOSTNAME;
  }

  let url = baseurl + "/api/v1/session";

  let res = http.post(url, JSON.stringify(sessionData), {
    headers: { "Content-Type": "application/json" },
  });

  submission(res.json().sessionId, res.json().token);
}

function submission(sessionId, token) {
  let url = baseurl + "/api/v1/session/" + sessionId + "/submission";

  let res = http.post(url, JSON.stringify(submissionData), {
    headers: {
      "Content-Type": "application/json",
      token: token,
    },
  });

  ready(sessionId, token);
}

function ready(sessionId, token) {
  let url = baseurl + "/api/v1/session/" + sessionId + "/submission/ready";

  let res = http.post(url, null, {
    headers: {
      token: token,
    },
  });

  check(res, {
    "is status 200": (r) => r.status === 200,
    "verify response text": (r) => r.body.includes(""),
  });
}
