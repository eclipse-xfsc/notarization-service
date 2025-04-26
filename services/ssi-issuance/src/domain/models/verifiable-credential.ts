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

import structuredClone from '@ungap/structured-clone';
import dayjs from 'dayjs';
import { merge } from 'lodash';
import { Mutable } from '../../infrastructure/utils/types/mutable';
import { Credential } from '../types/credential';
import { CredentialStatus } from '../types/credential-status';
import { CredentialData } from './credential-data.js';

export class VerifiableCredential {
  #defaultContext = new Set<string>(['https://www.w3.org/2018/credentials/v1']);
  #context = new Set<string>();
  #defaultTypes = new Set<string>(['VerifiableCredential']);
  #types = new Set<string>();
  #subject: Record<string, unknown> = {};
  #issuer: string;
  #holder: string;
  #issuanceDate: Date = new Date();
  #expirationDate: Date;
  #status: CredentialStatus;

  get status(): CredentialStatus {
    return this.#status;
  }

  static from(...sources: Record<string, unknown>[]) {
    const vc = new VerifiableCredential();

    const data = merge({}, ...sources);

    if (Array.isArray(data['@context'])) {
      vc.addContexts(data['@context']);
    }

    if (Array.isArray(data.type)) {
      vc.addTypes(data.type);
    }

    if (
      data.credentialSubject !== null &&
      typeof data.credentialSubject === 'object' &&
      !Array.isArray(data.credentialSubject)
    ) {
      vc.setSubject(data.credentialSubject as Record<string, unknown>);
    }

    return vc;
  }

  addContexts(contexts: string[]): this {
    contexts.forEach((context) => this.addContext(context));
    return this;
  }

  addContext(context: string): this {
    if (!this.#defaultContext.has(context)) {
      this.#context.add(context);
    }
    return this;
  }

  addTypes(types: string[]): this {
    types.forEach((type) => this.addType(type));
    return this;
  }

  addType(type: string | string[]): this {
    if (Array.isArray(type)) {
      return this.addTypes(type);
    }

    if (!this.#defaultTypes.has(type)) {
      this.#types.add(type);
    }
    return this;
  }

  setIssuer(issuer: string): this {
    this.#issuer = issuer;
    return this;
  }

  setHolder(holder: string): this {
    this.#holder = holder;
    return this;
  }

  setSubject(properties: Record<string, unknown>): this;
  setSubject(property: string, value: unknown): this;
  setSubject(
    properties: string | Record<string, unknown>,
    value?: unknown,
  ): this {
    if (typeof properties === 'string') {
      this.#subject[properties] = value;
    } else {
      Object.assign(this.#subject, structuredClone(properties));
    }

    return this;
  }

  setIssuanceDate(issuanceDate: Date | string): this {
    const date = dayjs(issuanceDate);
    if (date.isValid()) {
      this.#issuanceDate = date.toDate();
    } else {
      throw new TypeError(
        `'issuanceDate' parameters is of unexpected format. Should be a Date or a date string.`,
      );
    }
    return this;
  }

  setExpirationDate(validityInterval: string): this;
  setExpirationDate(expirationDate: Date): this;
  setExpirationDate(expirationDate: Date | string): this {
    if (typeof expirationDate === 'string') {
      // Try to parse the `validFor` string
      const duration = dayjs.duration(expirationDate);

      if (dayjs.isDuration(duration)) {
        const expirationDate = dayjs(this.#issuanceDate).add(duration);
        this.#expirationDate = expirationDate.toDate();
      } else {
        const date = dayjs(expirationDate);

        if (date.isValid()) {
          this.#expirationDate = date.toDate();
        } else {
          throw new TypeError(
            `'expirationDate' parameter is of unexpected format. Should be a Date, a date string or a ISO 8601 duration string`,
          );
        }
      }
    } else {
      this.#expirationDate = expirationDate;
    }

    return this;
  }

  setStatus(status: CredentialStatus): this {
    if (!status.id || !status.type) {
      throw new TypeError(
        `Credential status MUST contain 'id' and 'type'. See https://www.w3.org/TR/vc-data-model/#status for details.`,
      );
    }
    this.#status = structuredClone(status) as CredentialStatus;
    return this;
  }

  toPlain(): Credential {
    const subject: Record<string, unknown> = { ...this.#subject };

    if (this.#holder) {
      subject.id = this.#holder;
    }

    const result: Mutable<Credential> = {
      '@context': [...this.#defaultContext, ...this.#context],
      type: [...this.#defaultTypes, ...this.#types],
      issuer: this.#issuer,
      issuanceDate: this.#issuanceDate,
      credentialSubject: subject,
    };

    if (this.#expirationDate) {
      result.expirationDate = this.#expirationDate;
    }

    if (this.#status) {
      result.credentialStatus = this.#status as Mutable<CredentialStatus>;
    }

    // The type casting is done to make ACA-Py's and our type play well together
    return result as unknown as Credential;
  }
}
