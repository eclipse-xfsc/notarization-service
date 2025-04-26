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

import { Observable } from 'rxjs';
import { AbstractService } from '../abstract.service';
import { definitions } from '../types';
import { GetIssueCredentialV20RecordsParameters } from './types/get-issue-credential-v20-records-parameters.interface';

export class IssueCredentialV2RecordsService {
  constructor(private readonly makeRequest: AbstractService['makeRequest']) {}

  /**
   * Fetch all credential exchange records
   */
  getAll(
    params: GetIssueCredentialV20RecordsParameters,
  ): Observable<definitions['V20CredExRecordListResult']> {
    return this.makeRequest({
      method: 'get',
      url: '',
      params,
    });
  }

  /**
   * Fetch a single credential exchange record
   */
  get(
    credentialExchangeId: string,
  ): Observable<definitions['V20CredExRecordDetail']> {
    return this.makeRequest({
      method: 'get',
      url: credentialExchangeId,
    });
  }

  /**
   * Remove an existing credential exchange record
   */
  delete(
    credentialExchangeId: string,
  ): Observable<definitions['V20IssueCredentialModuleResponse']> {
    return this.makeRequest({
      method: 'delete',
      url: credentialExchangeId,
    });
  }

  /**
   * Send holder a credential
   */
  issue(
    credentialExchangeId: string,
    data: definitions['V20CredIssueRequest'],
  ): Observable<definitions['V20IssueCredentialModuleResponse']> {
    return this.makeRequest({
      method: 'post',
      url: `${credentialExchangeId}/issue`,
      data,
    });
  }

  /**
   * Send a problem report for credential exchange
   */
  problemReport(
    credentialExchangeId: string,
    data: definitions['V20CredIssueProblemReportRequest'],
  ): Observable<definitions['V20CredExRecordDetail']> {
    return this.makeRequest({
      method: 'post',
      url: `${credentialExchangeId}/problem-report`,
      data,
    });
  }

  /**
   * Send holder a credential offer in reference to a proposal with preview
   */
  sendOffer(
    credentialExchangeId: string,
    data: definitions['V20CredBoundOfferRequest'],
  ): Observable<definitions['V20IssueCredentialModuleResponse']> {
    return this.makeRequest({
      method: 'post',
      url: `${credentialExchangeId}/send-offer`,
      data,
    });
  }

  /**
   * Send issuer a credential request
   */
  sendRequest(
    credentialExchangeId: string,
    data: definitions['V20CredRequestRequest'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: `${credentialExchangeId}/send-request`,
      data,
    });
  }

  /**
   * Store a received credential
   */
  store(
    credentialExchangeId: string,
    data: definitions['V20CredStoreRequest'],
  ): Observable<definitions['V20CredExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: `${credentialExchangeId}/store`,
      data,
    });
  }
}
