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
import { PresentProofV1RecordsService } from './present-proof-v1-records.service';

@Injectable()
export class PresentProofV1Service extends AbstractService {
  public readonly records = new PresentProofV1RecordsService(
    this.makeRequest.bind(this),
  );
  protected readonly urlPrefix = 'present-proof';

  /**
   * Creates a presentation request not bound to any proposal or connection
   */
  createRequest(
    data: definitions['V10PresentationCreateRequestRequest'],
  ): Observable<definitions['V10PresentationExchange']> {
    return this.makeRequest({
      method: 'post',
      url: 'create-request',
      data,
    });
  }

  /**
   * Sends a presentation proposal
   */
  sendProposal(
    data: definitions['V10PresentationProposalRequest'],
  ): Observable<definitions['V10PresentationExchange']> {
    return this.makeRequest({
      method: 'post',
      url: 'send-proposal',
      data,
    });
  }

  /**
   * Sends a free presentation request not bound to any proposal
   */
  sendRequest(
    data: definitions['V10PresentationSendRequestRequest'],
  ): Observable<definitions['V10PresentationExchange']> {
    return this.makeRequest({
      method: 'post',
      url: 'send-request',
      data,
    });
  }
}
