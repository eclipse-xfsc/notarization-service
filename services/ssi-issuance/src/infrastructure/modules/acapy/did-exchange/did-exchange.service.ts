/****************************************************************************
 * Copyright 2023 Spherity GmbH
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
import { AcceptInvitationParameters } from './types/accept-invitation-parameters.interface';
import { AcceptRequestParameters } from './types/accept-request-parameters.interface';
import { CreateRequestParameters } from './types/create-request-parameters.interface';
import { ReceiveRequestParameters } from './types/receive-request-parameters.interface';

export class DIDExchangeService extends AbstractService {
  protected readonly urlPrefix = 'didexchange';

  createRequest(
    params: CreateRequestParameters,
  ): Observable<definitions['ConnRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'create-request',
      params,
    });
  }

  receiveRequest(
    params: ReceiveRequestParameters,
  ): Observable<definitions['ConnRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'receive-request',
      params,
    });
  }

  acceptInvitation(
    connectionId: string,
    params: AcceptInvitationParameters = {},
  ): Observable<definitions['ConnRecord']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/accept-invitation`,
      params,
    });
  }

  acceptRequest(
    connectionId: string,
    params: AcceptRequestParameters = {},
  ): Observable<definitions['ConnRecord']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/accept-request`,
      params,
    });
  }
}
