{{/*
Expand the name of the chart.
*/}}
{{- define "ssi-issuance.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "ssi-issuance.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "ssi-issuance.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "ssi-issuance.labels" -}}
helm.sh/chart: {{ include "ssi-issuance.chart" . }}
{{ include "ssi-issuance.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Istio labels
*/}}
{{- define "app.istioLabels" -}}
{{- if ((.Values.istio).injection).pod -}}
sidecar.istio.io/inject: "true"
{{- else if eq (((.Values.istio).injection).pod) false -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}
{{/*

{{/*
Selector labels
*/}}
{{- define "ssi-issuance.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ssi-issuance.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "ssi-issuance.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "ssi-issuance.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
