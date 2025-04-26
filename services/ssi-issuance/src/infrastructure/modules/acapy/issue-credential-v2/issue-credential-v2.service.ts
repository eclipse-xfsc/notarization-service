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

import { Injectable } from '@nestjs/common';
import { filter, Observable, ReplaySubject, take } from 'rxjs';
import { AbstractService } from '../abstract.service';
import { definitions } from '../types';
import { IssueCredentialV2RecordsService } from './issue-credential-v2-records.service';
import { CredentialExchangeRecordState } from './types/cred-exchange-record-state.enum';

@Injectable()
export class IssueCredentialV2Service extends AbstractService {
  protected readonly urlPrefix = 'issue-credential-2.0';

  protected readonly credentialExchangeRecords = new ReplaySubject<
    Required<definitions['V20CredExRecord']>
  >(
    this.config.issueCredentialV2.webhooks.buffer,
    this.config.issueCredentialV2.webhooks.timeWindow,
  );

  get events() {
    return this.credentialExchangeRecords;
  }

  readonly records = new IssueCredentialV2RecordsService(
    this.makeRequest.bind(this),
  );

  /**
   * Create credential from attribute values
   */
  create(
    data: definitions['V20IssueCredSchemaCore'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'create',
      data,
    });
  }

  /**
   * Create a credential offer, independent of any proposal or connection
   */
  createOffer(
    data: definitions['V20CredOfferConnFreeRequest'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'create-offer',
      data,
    });
  }

  /**
   * Send holder a credential, automating entire flow
   */
  send(
    data: definitions['V20CredExFree'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'send',
      data,
    });
  }

  /**
   * Send holder a credential offer, independent of any proposal
   */
  sendOffer(
    data: definitions['V20CredOfferRequest'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'create',
      data,
    });
  }

  /**
   * Send issuer a credential proposal
   */
  sendProposal(
    data: definitions['V20CredExFree'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'send-proposal',
      data,
    });
  }

  /**
   * Send issuer a credential request not bound to an existing thread. Indy credentials cannot start at a request
   */
  sendRequest(
    data: definitions['V20CredRequestFree'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'create',
      data,
    });
  }

  /**
   *
   */
  handleWebhook(
    credentialExchangeRecord: Required<definitions['V20CredExRecord']>,
  ) {
    this.credentialExchangeRecords.next(credentialExchangeRecord);
  }

  /**
   *
   */
  async waitForState(
    { cred_ex_id }: Required<definitions['V20CredExRecord']>,
    state: CredentialExchangeRecordState,
    timeout = 2000,
  ) {
    return Promise.race([
      new Promise((_resolve, reject) => setTimeout(reject, timeout)),
      new Promise((resolve) => {
        this.credentialExchangeRecords
          .pipe(
            filter(
              (credExRecord) =>
                credExRecord.cred_ex_id === cred_ex_id &&
                credExRecord.state === (state as string),
            ),
            take(1),
          )
          .subscribe((presExRecord) => {
            resolve(presExRecord);
          });
      }),
    ]);
  }
}
