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
import { SearchCredentialDefinitionsParameters } from './types/search-credential-definitions-parameters.interface';
import { SendCredentialDefinitionParameters } from './types/send-credential-definition-parameters.interface';

@Injectable()
export class CredentialDefinitionService extends AbstractService {
  protected readonly urlPrefix = 'credential-definitions';

  /**
   * Sends a credential definition to the ledger
   */
  sendCredentialDefinition(
    data: definitions['CredentialDefinitionSendRequest'],
    params?: SendCredentialDefinitionParameters,
  ): Observable<definitions['TxnOrCredentialDefinitionSendResult']> {
    return this.makeRequest({
      method: 'post',
      url: '',
      data,
      params,
    });
  }

  /**
   * Search for matching credential definitions that agent originated
   */
  created(
    params: SearchCredentialDefinitionsParameters = {},
  ): Observable<definitions['CredentialDefinitionsCreatedResult']> {
    return this.makeRequest({
      method: 'get',
      url: 'created',
      params,
    });
  }

  /**
   * Gets a credential definition from the ledger
   */
  get(
    credentialDefinitionId: string,
  ): Observable<definitions['CredentialDefinitionGetResult']> {
    return this.makeRequest({
      method: 'get',
      url: credentialDefinitionId,
    });
  }

  /**
   * Writes a credential definition non-secret record to the wallet
   */
  writeRecord(
    credentialDefinitionId: string,
  ): Observable<definitions['CredentialDefinitionGetResult']> {
    return this.makeRequest({
      method: 'post',
      url: credentialDefinitionId,
    });
  }
}
