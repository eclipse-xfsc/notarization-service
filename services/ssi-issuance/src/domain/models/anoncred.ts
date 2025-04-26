/****************************************************************************
 * Copyright 2022 Spherity GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

import { definitions } from '../../infrastructure/modules/acapy';

export class Anoncred {
  #connectionId: string;
  #issuer: string;
  #schemaId: string;
  #credentialDefinitionId: string;
  #attributes = new Map<string, unknown>();

  static fromTemplate(template: Record<string, unknown>): Anoncred {
    const ac = new Anoncred();

    if (Array.isArray(template.attributes)) {
      template.attributes.forEach((attribute) => {
        ac.#attributes.set(attribute, undefined);
      });
    }

    return ac;
  }

  addAttribute(attribute: string, value: unknown): this {
    return this.addAttributes({ [attribute]: value });
  }

  addAttributes(attributes: Record<string, unknown>): this {
    for (const [attribute, value] of Object.entries(attributes)) {
      if (this.#attributes.has(attribute)) {
        this.#attributes.set(attribute, value);
      } else {
        // Warning about extraneous attribute
      }
    }
    return this;
  }

  setIssuer(issuer: string): this {
    this.#issuer = issuer;
    return this;
  }

  setConnection(connection: definitions['ConnRecord']): this {
    if (connection?.connection_id) {
      this.#connectionId = connection.connection_id;
    }
    return this;
  }

  setConnectionId(connectionId: string): this {
    if (connectionId) {
      this.#connectionId = connectionId;
    }
    return this;
  }

  setSchemaId(schemaId: string) {
    this.#schemaId = schemaId;
    return this;
  }

  setCredentialDefinitionId(credentialDefinitionId: string) {
    this.#credentialDefinitionId = credentialDefinitionId;
    return this;
  }

  toPlain() {
    const attributes: Record<string, unknown>[] = [];
    const result: Record<string, unknown> = {
      credential_proposal: { attributes },
    };

    for (const [key, value] of this.#attributes.entries()) {
      attributes.push({
        name: key,
        value,
      });
    }

    if (this.#connectionId) {
      result.connection_id = this.#connectionId;
    }

    if (this.#issuer) {
      result.issuer_did = this.#issuer;
    }

    if (this.#schemaId) {
      result.schema_id = this.#schemaId;
    }

    if (this.#credentialDefinitionId) {
      result.cred_def_id = this.#credentialDefinitionId;
    }

    return result;
  }
}
