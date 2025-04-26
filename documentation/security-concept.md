# Security Concept

<!-- TOC GitLab -->

- [Introduction](#introduction)
- [Project Description and Goals](#project-description-and-goals)
- [Purpose of the Document](#purpose-of-the-document)
- [Threat model](#threat-model)
- [Key inventory](#key-inventory)

<!-- /TOC -->

## Introduction

The document discusses security concepts around GAIA-X Notarization Service.
The document assumes a basic knowledge of security methodologies and practices in the audience reading the document and does not explain these topics in detail.

## Project Description and Goals

The goal of GAIA-X Notarization Service project is to implement a service which will issue verifiable credentials to respective holders.

## Purpose of the Document

The intent of this document is to provide an overview of implemented functionality as well as of information security principles and concepts taken into account in implementation of the Notarization Service project.

## Threat model

The following is a thread model of the Notarization API system, created using [OWASP Threat Dragon](https://www.threatdragon.com):

- As threat model: [Notarization-API-Threat-Model.json](./Notarization-API-Threat-Model.json)
- As generated pdf report: [Notarization-API-Threat-Model.pdf](./Notarization-API-Threat-Model.pdf)

## Key inventory

The following key inventory is provided to complement the security knowledge:

| Component                                         | Identifier               | Type                                       | Protection | Note                                                                                                              |
| ------------------------------------------------- | ------------------------ | ------------------------------------------ | ---------- | ----------------------------------------------------------------------------------------------------------------- |
| RabbitMQ                                          | TLS Server Certificate   | Private Key                                | Vault      |                                                                                                                   |
| Request Processing, Issuance Controller, RabbitMQ | RabbitMQ Credential      | Client Certificate+Private Key or Password | Vault      | Depending on Cluster Configuration. RabbitMQ only if password is used.                                            |
| Request Processing                                | DB Credential            | Client Certificate+Private Key or Password | Vault      | Type depending on DB Configuration                                                                                |
| Request Processing                                | Notary IdP client_secret | Symmetric Key                              | Vault      | OIDC Client Configuration also provides other authentication means against the IdP. See Quarkus docs for details. |
| Revocation                                        | DB Credential            | Client Certificate+Private Key or Password | Vault      | Type depending on DB Configuration                                                                                |
| Issuance Controller                               | ACA-Py API Key           | Symmetric Key                              | Vault      |                                                                                                                   |
| ACA-Py                                            | ACA-Py API Key            | Symmetric Key                              | Vault      |                                                                                                                   |
| ACA-Py                                            | Wallet Key               | Symmetric Key                              | Vault      | Used to derive encryption key for database incl. DID secrets                                                      |
| eID Task                                          | DB Credential            | Client Certificate+Private Key or Password | Vault      | Type depending on DB Configuration                                                                                |
| eID Task                                          | eID-IdP client_secret    | Symmetric Key                              | Vault      | OIDC Client Configuration also provides other authentication means against the IdP. See Quarkus docs for details. |
