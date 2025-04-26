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
import { GetAllDidsParameters } from './types/get-all-dids-parameters.interface';

export class DidService {
  constructor(private readonly makeRequest: AbstractService['makeRequest']) {}

  /**
   * List wallet DIDs
   */
  getAll(
    params: GetAllDidsParameters = {},
  ): Observable<definitions['DIDList']> {
    return this.makeRequest({
      method: 'get',
      url: 'did',
      params,
    });
  }

  /**
   * Create a local DID
   */
  create(data: definitions['DIDCreate']): Observable<definitions['DIDResult']> {
    return this.makeRequest({
      method: 'post',
      url: 'did/create',
      data,
    });
  }

  /**
   * Rotate keypair for a DID not posted to the ledger
   */
  rotateLocalKeypair(
    did: string,
  ): Observable<definitions['WalletModuleResponse']> {
    return this.makeRequest({
      method: 'patch',
      url: 'did/local/rotate-keypair',
      params: { did },
    });
  }

  /**
   * Fetch the current public DID
   */
  getPublicDid(): Observable<definitions['DIDResult']> {
    return this.makeRequest({
      method: 'get',
      url: 'did/public',
    });
  }

  /**
   * Assign the current public DID
   */
  setPublicDid(did: string): Observable<definitions['DIDResult']> {
    return this.makeRequest({
      method: 'post',
      url: 'did/public',
      params: { did },
    });
  }
}
