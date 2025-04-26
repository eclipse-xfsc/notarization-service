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
import { GetIssueCredentialV10RecordsParameters } from './types/get-issue-credential-v10-records-parameters.interface';

export class IssueCredentialV1RecordsService {
  constructor(private readonly makeRequest: AbstractService['makeRequest']) {}

  /**
   * Fetch all credential exchange records
   */
  getAll(
    params: GetIssueCredentialV10RecordsParameters,
  ): Observable<definitions['V10CredentialExchangeListResult']> {
    return this.makeRequest({
      method: 'get',
      url: 'records',
      params,
    });
  }

  /**
   * Fetch a single credential exchange record
   */
  get(
    credentialExchangeId: string,
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'get',
      url: `records/${credentialExchangeId}`,
    });
  }

  /**
   * Remove an existing credential exchange record
   */
  delete(
    credentialExchangeId: string,
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'delete',
      url: `records/${credentialExchangeId}`,
    });
  }

  /**
   * Send holder a credential
   */
  issue(
    credentialExchangeId: string,
    data: definitions['V10CredentialIssueRequest'],
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${credentialExchangeId}/issue`,
      data,
    });
  }

  /**
   * Send a problem report for credential exchange
   */
  problemReport(
    credentialExchangeId: string,
    data: definitions['V10CredentialProblemReportRequest'],
  ): Observable<definitions['IssueCredentialModuleResponse']> {
    return this.makeRequest({
      method: 'get',
      url: `records/${credentialExchangeId}/problem-report`,
      data,
    });
  }

  /**
   * Send holder a credential offer in reference to a proposal with preview
   */
  sendOffer(
    credentialExchangeId: string,
    data: definitions['V10CredentialBoundOfferRequest'],
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${credentialExchangeId}/send-offer`,
      data,
    });
  }

  /**
   * Send issuer a credential request
   */
  sendRequest(
    credentialExchangeId: string,
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'get',
      url: `records/${credentialExchangeId}/send-request`,
    });
  }

  /**
   * Store a received credential
   */
  store(
    credentialExchangeId: string,
    data: definitions['V10CredentialStoreRequest'],
  ): Observable<definitions['V10CredentialExchange']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${credentialExchangeId}/store`,
      data,
    });
  }
}
