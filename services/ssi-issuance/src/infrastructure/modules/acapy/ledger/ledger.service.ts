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
import { MultipleService } from './multiple.service';
import { TaaService } from './taa.service';
import { GetDidEndpointParameters } from './types/get-did-endpoint-parameters.interface';
import { GetDidVerkeyParameters } from './types/get-did-verkey-parameters.interface';
import { GetNymRoleParameters } from './types/get-nym-role-parameters.interface';
import { RegisterNymParameters } from './types/register-nym-parameters.interface';

@Injectable()
export class LedgerService extends AbstractService {
  readonly multiple = new MultipleService(this.makeRequest.bind(this));
  readonly taa = new TaaService(this.makeRequest.bind(this));
  protected readonly urlPrefix = 'ledger';

  /**
   * Get the endpoint for a DID from the ledger.
   */
  getDidEndpoint(
    params: GetDidEndpointParameters,
  ): Observable<definitions['GetDIDEndpointResponse']> {
    return this.makeRequest({
      method: 'get',
      url: 'did-endpoint',
      params,
    });
  }

  /**
   * Get the verkey for a DID from the ledger.
   */
  getDidVerkey(
    params: GetDidVerkeyParameters,
  ): Observable<definitions['GetDIDVerkeyResponse']> {
    return this.makeRequest({
      method: 'get',
      url: 'did-verkey',
      params,
    });
  }

  /**
   * Get the role from the NYM registration of a public DID.
   */
  getNymRole(
    params: GetNymRoleParameters,
  ): Observable<definitions['GetNymRoleResponse']> {
    return this.makeRequest({
      method: 'get',
      url: 'get-nym-role',
      params,
    });
  }

  /**
   * Send a NYM registration to the ledger.
   */
  registerNym(
    params: RegisterNymParameters,
  ): Observable<definitions['TxnOrRegisterLedgerNymResponse']> {
    return this.makeRequest({
      method: 'post',
      url: 'register-nym',
      params,
    });
  }

  /**
   * Rotate key pair for public DID.
   */
  rotatePublicDidKeypair(): Observable<definitions['LedgerModulesResult']> {
    return this.makeRequest({
      method: 'patch',
      url: 'rotate-public-did-keypair',
    });
  }
}
