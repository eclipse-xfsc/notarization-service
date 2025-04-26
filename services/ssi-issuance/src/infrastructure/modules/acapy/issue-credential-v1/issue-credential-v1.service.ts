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
import { Observable } from 'rxjs';
import { AbstractService } from '../abstract.service';
import { definitions } from '../types';
import { IssueCredentialV1RecordsService } from './issue-credential-v1-records.service';

@Injectable()
export class IssueCredentialV1Service extends AbstractService {
  protected readonly urlPrefix = 'issue-credential';

  public readonly records = new IssueCredentialV1RecordsService(
    this.makeRequest.bind(this),
  );

  /**
   * Send holder a credential, automating entire flow
   */
  create(
    data: definitions['V10CredentialCreate'],
  ): Observable<definitions['V10CredentialExchange']> {
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
    data: definitions['V10CredentialConnFreeOfferRequest'],
  ): Observable<definitions['V10CredentialExchange']> {
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
    data: definitions['V10CredentialProposalRequestMand'],
  ): Observable<definitions['V10CredentialExchange']> {
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
    data: definitions['V10CredentialFreeOfferRequest'],
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'post',
      url: 'send-offer',
      data,
    });
  }

  /**
   * Send issuer a credential proposal
   */
  sendProposal(
    data: definitions['V10CredentialProposalRequestOpt'],
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'post',
      url: 'send-proposal',
      data,
    });
  }
}
