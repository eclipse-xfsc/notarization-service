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
import { GetPresentProofV1RecordCredentialsParameters } from './types/get-present-proof-v1-record-credentials-parameters.interface';
import { GetPresentProofV10RecordsParameters } from './types/get-present-proof-v1-records-parameters.interface';

export class PresentProofV1RecordsService {
  constructor(private readonly makeRequest: AbstractService['makeRequest']) {}

  /**
   * Fetch all present-proof exchange records
   */
  getAll(
    params: GetPresentProofV10RecordsParameters,
  ): Observable<definitions['V10PresentationExchangeList']> {
    return this.makeRequest({
      method: 'get',
      url: `records`,
      params,
    });
  }

  /**
   * Fetch a single presentation exchange record
   */
  get(
    presentationExchangeId: string,
  ): Observable<definitions['V10PresentationExchange']> {
    return this.makeRequest({
      method: 'get',
      url: `records/${presentationExchangeId}`,
    });
  }

  /**
   * Remove an existing presentation exchange record
   */
  delete(
    presentationExchangeId: string,
  ): Observable<definitions['V10PresentProofModuleResponse']> {
    return this.makeRequest({
      method: 'delete',
      url: `records/${presentationExchangeId}`,
    });
  }

  /**
   * Fetch credentials for a presentation request from wallet
   */
  credentials(
    presentationExchangeId: string,
    params: GetPresentProofV1RecordCredentialsParameters,
  ): Observable<definitions['IndyCredPrecis'][]> {
    if (
      typeof params?.extraQuery === 'object' &&
      params.extraQuery !== null &&
      !Array.isArray(params.extraQuery)
    ) {
      params.extraQuery = JSON.stringify(params.extraQuery);
    }

    return this.makeRequest({
      method: 'get',
      url: `records/${presentationExchangeId}/credentials`,
      params,
    });
  }

  /**
   * Send a problem report for presentation exchange
   */
  problemReport(
    presentationExchangeId: string,
    data: definitions['V10PresentationProblemReportRequest'],
  ): Observable<definitions['V10PresentProofModuleResponse']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${presentationExchangeId}/problem-report`,
      data,
    });
  }

  /**
   * Sends a proof presentation
   */
  sendPresentation(
    presentationExchangeId: string,
    data: definitions['IndyCredPrecis'],
  ): Observable<definitions['V10PresentationExchange']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${presentationExchangeId}/send-presentation`,
      data,
    });
  }

  /**
   * Sends a presentation request in reference to a proposal
   */
  sentRequest(
    presentationExchangeId: string,
    data: definitions['V10PresentationSendRequestToProposal'],
  ): Observable<definitions['V10PresentationExchange']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${presentationExchangeId}/send-request`,
      data,
    });
  }

  /**
   * Verify a received presentation
   */
  verifyPresentation(
    presentationExchangeId: string,
  ): Observable<definitions['V10PresentationExchange']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${presentationExchangeId}/verify-presentation`,
    });
  }
}
