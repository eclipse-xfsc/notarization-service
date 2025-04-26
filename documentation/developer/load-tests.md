# Load Tests

To demonstrate high workload scenarios, the open source tool [K6](https://k6.io) is used.

K6 can be run as single binary or with the help of Docker.
Detailed information about installation, configuring and running the tool can be found in the [official documentation](https://k6.io/docs/).

Third party components, like `Keycloak` or `Postgres`, must be chosen in a way
that they don't limit the performance of the system.

## Result Evaluation

After completion, each test writes the results with the summary to `stdout` and produces a HTML report in the same directory (e.g. `summary-simple.html`).
It prints basic statistics about each metric (e.g. mean, median, percentiles, etc) and the results of various checks (if present).
An example output could look like the following:

```sh
✓ create holder DID status code is 200
✓ create holder invitation status code is 200

checks.........................: 100.00% ✓ 200       ✗ 0  
data_received..................: 122 kB  20 kB/s
data_sent......................: 40 kB   6.4 kB/s
http_req_blocked...............: avg=12µs     min=2.78µs  med=5.18µs   max=1.28ms   p(90)=7.32µs   p(95)=8.82µs  
http_req_connecting............: avg=635ns    min=0s      med=0s       max=127.19µs p(90)=0s       p(95)=0s      
http_req_duration..............: avg=30.36ms  min=11.91ms med=29.2ms   max=86.81ms  p(90)=42.44ms  p(95)=48.36ms 
  { expected_response:true }...: avg=30.36ms  min=11.91ms med=29.2ms   max=86.81ms  p(90)=42.44ms  p(95)=48.36ms 
http_req_failed................: 0.00%   ✓ 0         ✗ 200
http_req_receiving.............: avg=150.58µs min=59.34µs med=108.72µs max=2.24ms   p(90)=236.12µs p(95)=296.01µs
http_req_sending...............: avg=34.16µs  min=18.21µs med=29.98µs  max=575.11µs p(90)=42.05µs  p(95)=47.21µs 
http_req_tls_handshaking.......: avg=0s       min=0s      med=0s       max=0s       p(90)=0s       p(95)=0s      
http_req_waiting...............: avg=30.18ms  min=11.52ms med=29.03ms  max=86.45ms  p(90)=42.28ms  p(95)=48.12ms 
http_reqs......................: 200     32.506941/s
iteration_duration.............: avg=61.48ms  min=41.85ms med=57.11ms  max=161.35ms p(90)=71.05ms  p(95)=88.66ms 
iterations.....................: 100     16.25347/s
```

The documentation of the metrics above can be found on the [project website](https://k6.io/docs/using-k6/metrics/#http-specific-built-in-metrics).

## Testing the scalability

To demonstrate the scalability of the project, four load test scenarios were
developed. For the tests, the following tools are required:

- K6
- kubectl
- kubectl plugin resource-capacity (`kubectl krew install resource-capacity`)
- watch command (on Linux or something similar)

Assuming a three node test cluster, the general steps to reproduce the tests are:

- Temporarily remove (drain) two nodes from the cluster.
- Load test the single node cluster and write down the results.
- Add one node and repeat the tests.
- Add the third node and repeat the tests.

Hint: make sure to also scale up the involved infrastructure (e.g. Keycloak) as
well to prevent bottlenecks.

### Basic steps to prepare the cluster

The following commands and comments help to get information about the cluster,
to drain all nodes except one and to make sure that only one replica of each
service is running.

```sh
# Show the available labels of the nodes.
kubectl get nodes --show-labels
# Get all worker nodes (exclude control plane etc.).
kubectl get nodes -l node.kubernetes.io/instance-type=<instance-type>
# Example call:
kubectl get nodes -l node.kubernetes.io/instance-type=b4fbb8fa-f8e4-486a-a553-4cfd6b740853
# Drain all nodes except of one.
# It is important that the node has the status 'SchedulingDisabled' and that
# new replicas will not be created on this specific node.
kubectl drain k8s-<node-id>
# Check if all pods were removed.
kubectl get pods --field-selector spec.nodeName=k8s-<node-id> -n <namespace> -o wide
# Make sure, only one replica of each load-tested service is present.
kubectl scale deployment -n <namespace> --replicas=1 oidc-identity-resolver profile request-processing revocation scheduler ssi-issuance-controller not-acapy
# In addition, we also scaled keycloak and the ingress controller.
kubectl scale statefulset -n <namespace> --replicas=1 keycloak 
kubectl scale deployment -n <namespace> --replicas=1 ingress-nginx-controller
```

Now, the desired load tests can be executed. In a different shell, issue the
following command in order to check the metrics of the cluster:

```sh
watch -n 1 "kubectl resource-capacity --node-labels node.kubernetes.io/instance-type=<instance-type> -n <namespace> --util --pods"
```

### Load test "Management"

In this test, users that are authorized as notaries are fetching all available
requests. The test can be run with the following command:

```sh
k6 run --env KC_CLIENT_SECRET=<CLIENT_SECRET> \
       --env KC_CLIENT_ID=<CLIENT_ID> \
       --env KC_USERNAME=<NOTARY_USERNAME> \
       --env KC_PASSWORD=<NOTARY_PASSWORD> \
       lt-management.js
```

### Load test "Submission"

In this test, there are three subsequent API calls (create a session, issue a
submission and set the state to "ready"). Run the test with the following
command:

```sh
k6 run lt-submission.js
```

### Load test "Identity Encryption"

The encryption happens while assigning the identity in the `NotarizationRequestStore` and the encrypted data is saved in the request processing database (in the table `requestor_identity`).

The test uses the Keycloak instance deployed under <https://idp-not.gxfs.dev>, so make sure to provide the correct credentials.

Run the test with the following command:

```sh
k6 run \
    -e KC_CLIENT_SECRET=<client-secret> \
    -e KC_USER_PASSWORD=<user-password> \
    ./lt-identity-encryption.js
```

### Load test "Issuance"

Before you're able to run this load test, you have to provide the base URL of
the credential receiver via the environment variable `BASEURL_ACAPY_HOLDER`.
If the credential receiver is protected by an API key, the API key must be provided
also as environment variable (`ACAPY_HOLDER_API_KEY`).
If you want to skip holder communication, you can use the env variable
`SKIP_HOLDER_COMMUNICATION` what should be done because usually you don't want to load
test the credential receiver.
If you only want to skip waiting for receiving the credential, you can use the env variable
`SKIP_WAITING_FOR_CREDENTIAL`.
As the endpoints of the issuance controller aren't publicly available, for this load 
test the forwarding of relevant ports with kubectl is needed.
The commands can look like:

```bash
kubectl -n lt-not-api port-forward service/ssi-issuance-controller 8080:80
# Run the load test without holder communication:
k6 run --env SKIP_HOLDER_COMMUNICATION=true lt-issuance.js
# Run the load test with your credential receiver
k6 run --env BASEURL_ACAPY_HOLDER=<BASE_URL_OF_HOLDER> --env ACAPY_HOLDER_API_KEY=<YOUR_API_KEY> --env SKIP_WAITING_FOR_CREDENTIAL=true ./lt-issuance.js
# Run the load test with the holder ACA-Py deployed at https://gaiax-4-api.acapy.spherity.io
k6 run --env ACAPY_HOLDER_API_KEY=<API_KEY> --env SKIP_WAITING_FOR_CREDENTIAL=true ./lt-issuance.js
```

### Add an additional node and repeat the tests

After the load tests on a single node are finished, the steps below can be used
to add additional nodes again.

```sh
# Make node available again for deploying pods.
kubectl uncordon k8s-<node-id>
# Increase the replica count by one and make sure the services are ready.
kubectl scale deployment -n <namespace> --replicas=<number-of-nodes> oidc-identity-resolver profile request-processing revocation scheduler ssi-issuance-controller not-acapy
# In addition, we also scale up keycloak and the ingress controller.
kubectl scale statefulset -n <namespace> --replicas=<number-of-nodes> keycloak 
kubectl scale deployment -n <namespace> --replicas=<number-of-nodes> ingress-nginx-controller

# Check if the correct number of pods is present on the different nodes.
kubectl get pods -o wide
```

If all settings are as desired, the load tests can be run again and the results
can be compared with the previous ones.
