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
import { DidService } from './did.service';

@Injectable()
export class WalletService extends AbstractService {
  protected readonly urlPrefix = 'wallet';

  readonly did = new DidService(this.makeRequest.bind(this));

  /**
   * Query DID endpoint in wallet
   */
  getDidEndpoint(did: string): Observable<definitions['DIDEndpoint']> {
    return this.makeRequest({
      method: 'get',
      url: 'get-did-endpoint',
      params: { did },
    });
  }

  /**
   * Update endpoint in wallet and on ledger if posted to it
   */
  setDidEndpoint(
    data: definitions['DIDEndpointWithType'],
  ): Observable<definitions['WalletModuleResponse']> {
    return this.makeRequest({
      method: 'post',
      url: 'set-did-endpoint',
      data,
    });
  }
}
