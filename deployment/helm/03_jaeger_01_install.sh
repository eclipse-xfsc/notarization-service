#!/bin/bash

source ./readenv.sh

helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
helm install jaeger --namespace "$NOT_API_NAMESPACE" -f ./jaeger/values.yaml jaegertracing/jaeger
