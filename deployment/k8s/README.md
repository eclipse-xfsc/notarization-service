# Deployment

## Folder Structure

| Folder        | Description                                                                               |
| ------------- | ----------------------------------------------------------------------------------------- |
| base          | Contains base k8s resources that can be overwritten by environment specific resources.    |
| base/build    | Contains generated Kubernetes resources.                                                  |
| base/configs  | Contains the default configuration of the notarization system as env files.               |
| base/secrets  | Contains some default secrets of the notarization system.                                 |
| overlays      | Contains environment specific resources.                                                  |

Following environments are currently set up and can be deployed to the test cluster:

* load-tests
* staging

For those environments, configs, secrets and ingress resources are provided in their specific folders (see `overlays`).

## Deployment via kustomize

### Prerequisites

Following third-party services are needed:

* PostgreSQL (Databases needed for  `request-processing`, `oidc-identity-resolver`, `profile`, `revocation` and the `scheduler`)
* RabbitMQ
* Keycloak or a similar IdP
* Digital Signature Service

## Deployment on Test-Cluster

Before you're able to deploy the notarization system, set your secrets in `overlays/${ENVIRONMENT}/secrets/*.secrets`.

Afterwards you can run the following commands to generate the Kubernetes resources and deploy them:

```bash
$ kubectl kustomize overlays/${ENVIRONMENT} > notarization-system.yml
$ kubectl apply -f notarization-system.yml --namespace=${YOUR_NAMESPACE}
```

### Customize the deployment resources

If you want to deploy the notarization system to your own cluster or want to add a new environment, you have to install the third-party components that are mentioned in the `Prerequisites` section.
Afterwards, you can add your own overlay for your specific environment. For such an overlay, you need to create a folder in `overlays/${YOUR_OVERLAY}` and add a `kustomization.yml`:

```yaml
bases:
- ../../base

patchesStrategicMerge:
- patches/configs.yml
- patches/secrets.yml

resources:
- ingress/ingress.yml
```

Now you have to create the specified files, set the config properties and secrets and adjust the Ingress resource (as it was done for the other overlays). Finally you can run:

```bash
$ kubectl kustomize overlays/${YOUR_OVERLAY} > notarization-system.yml
$ kubectl apply -f notarization-system.yml --namespace=${YOUR_NAMESPACE}
```

# TLS protected endpoints

To protect the notarization system endpoints, you can specify in the Ingress resource a secret that contains a TLS private key and certificate (see secret of type `kubernetes.io/tls`).
An appropriate Ingress resource can look like the following:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: notarization-system
  annotations:
    nginx.ingress.kubernetes.io/use-regex: "true"
    kubernetes.io/ingress.class: "nginx"
spec:
  rules:
  - host: request-lt-not.gxfs.dev
    http:
      paths:
      - backend:
          service:
            name: request-processing
            port:
              number: 80
        pathType: Prefix
        path: /.*
  ...
  tls:
  - hosts:
    - request-lt-not.gxfs.dev
    ...
    secretName: wildcard-gxfs-dev
```

whereby `wildcard-gxfs-dev` is a secret of the type `kubernetes.io/tls` that contains keys named `tls.crt` and `tls.key`.

A common ingress controller, like Nginx supports various TLS protocols. On the test cluster, the protocols `TLSv1.2` and `TLSv1.3` are configured.
