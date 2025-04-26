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
import { first, Observable, ReplaySubject } from 'rxjs';
import { AbstractService } from '../abstract.service';
import { definitions } from '../types';
import { PresentProofV2RecordsService } from './present-proof-v2-records.service';
import { PresentationExchangeRecordState } from './types/pres-exchange-record-state.enum';

@Injectable()
export class PresentProofV2Service extends AbstractService {
  readonly records = new PresentProofV2RecordsService(
    this.makeRequest.bind(this),
  );
  protected readonly urlPrefix = 'present-proof-2.0';
  protected readonly presentationExchangeRecords = new ReplaySubject<
    Required<definitions['V20PresExRecord']>
  >(
    this.config.presentProofV2.webhooks.buffer,
    this.config.presentProofV2.webhooks.timeWindow,
  );

  /**
   * Creates a presentation request not bound to any proposal or connection
   */
  createRequest(
    data: definitions['V20PresCreateRequestRequest'],
  ): Observable<definitions['V20PresExRecord']> {
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
    data: definitions['V20PresProposalRequest'],
  ): Observable<definitions['V20PresExRecord']> {
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
    data: definitions['V20PresSendRequestRequest'],
  ): Observable<definitions['V20PresExRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'send-request',
      data,
    });
  }

  /**
   *
   */
  handleWebhook(
    presentationExchangeRecord: Required<definitions['V20PresExRecord']>,
  ) {
    this.presentationExchangeRecords.next(presentationExchangeRecord);
  }

  get events() {
    return this.presentationExchangeRecords;
  }

  /**
   *
   */
  async waitForState(
    { pres_ex_id, connection_id }: definitions['V20PresExRecord'],
    state: PresentationExchangeRecordState,
    timeout = 2000,
  ) {
    return Promise.race([
      new Promise((_resolve, reject) => setTimeout(reject, timeout)),
      new Promise<Required<definitions['V20PresExRecord']>>((resolve) => {
        this.presentationExchangeRecords
          .pipe(
            first((presExRecord) => {
              const match =
                presExRecord.pres_ex_id === pres_ex_id &&
                (connection_id
                  ? presExRecord.connection_id === connection_id
                  : true) &&
                presExRecord.state === (state as string);
              return match;
            }),
          )
          .subscribe((presExRecord) => {
            resolve(presExRecord);
          });
      }),
    ]);
  }
}
