import http from "k6/http";
import exec from 'k6/execution';

export function authenticate(
  tokenEndPoint,
  grantType,
  clientId,
  clientSecret,
  username,
  password
) {
  const requestBody = {
    grant_type: grantType,
    client_id: clientId,
    client_secret: clientSecret,
    username: username,
    password: password,
  };

  const response = http.post(tokenEndPoint, requestBody);

  if (response.status == 200) {
    return response.json();
  } else {
    let errorMsg = new String("Unable to receive access token from IdP. Status Code: " + response.status);
    console.log(errorMsg);
    exec.test.abort();
  }
}
