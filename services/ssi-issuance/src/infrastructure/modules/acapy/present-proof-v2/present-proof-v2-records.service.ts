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
import { GetRecordCredentialsParameters } from './types/get-record-credentials-parameters.interface';
import { GetRecordsParameters } from './types/get-records-parameters.interface';

export class PresentProofV2RecordsService {
  constructor(private readonly makeRequest: AbstractService['makeRequest']) {}

  /**
   * Fetch all present-proof exchange records
   */
  async getAll(params: GetRecordsParameters) {
    return this.makeRequest({
      method: 'get',
      url: 'records',
      params,
    });
  }

  /**
   * Fetch a single presentation exchange record
   */
  get(
    presentationExchangeId: string,
  ): Observable<definitions['V20PresExRecord']> {
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
  ): Observable<definitions['V20PresentProofModuleResponse']> {
    return this.makeRequest({
      method: 'delete',
      url: `records/${presentationExchangeId}`,
    });
  }

  /**
   * Fetch credentials from wallet for presentation request
   */
  credentials(
    presentationExchangeId: string,
    params: GetRecordCredentialsParameters = {},
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
    data: definitions['V20PresProblemReportRequest'],
  ): Observable<definitions['V20PresentProofModuleResponse']> {
    return this.makeRequest({
      method: 'post',
      url: `records/${presentationExchangeId}/problem-report`,
      data,
    });
  }

  /**
   * Sends a proof presentation
   */
  sendRecordPresentation(
    presentationExchangeId: string,
    data: definitions['V20PresSpecByFormatRequest'],
  ): Observable<definitions['V20PresExRecord']> {
    return this.makeRequest({
      method: 'get',
      url: `records/${presentationExchangeId}/send-presentation`,
      data,
    });
  }

  /**
   * Sends a presentation request in reference to a proposal
   */
  sendRecordRequest(
    presentationExchangeId: string,
    data: definitions['V20PresentationSendRequestToProposal'],
  ): Observable<definitions['V20PresExRecord']> {
    return this.makeRequest({
      method: 'get',
      url: `records/${presentationExchangeId}/send-request`,
      data,
    });
  }

  /**
   * Verify a received presentation
   */
  verifyRecordPresentation(
    presentationExchangeId: string,
  ): Observable<definitions['V20PresExRecord']> {
    return this.makeRequest({
      method: 'get',
      url: `records/${presentationExchangeId}/verify-presentation`,
    });
  }
}
