#!/bin/bash

source ./readenv.sh

helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm install otel-collector --namespace "$NOT_API_NAMESPACE" -f ./otel-collector/values.yaml open-telemetry/opentelemetry-collector
