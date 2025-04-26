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
import { SearchSchemasParameters } from './types/search-schemas-parameters.interface';

@Injectable()
export class SchemasService extends AbstractService {
  protected readonly urlPrefix = 'schemas';

  /**
   * Sends a schema to the ledger
   */
  sendSchema(data: definitions['SchemaSendRequest']): Observable<
    // Type union needed because of weird response from ACA-Py
    // See https://github.com/hyperledger/aries-cloudagent-python/blob/main/aries_cloudagent/messaging/schemas/routes.py#L275-L281
    definitions['TxnOrSchemaSendResult'] &
      Partial<definitions['SchemaSendResult']>
  > {
    return this.makeRequest({
      method: 'post',
      data,
    });
  }

  /**
   * Search for matching schema that agent originated
   */
  search(
    params: SearchSchemasParameters,
  ): Observable<definitions['SchemasCreatedResult']> {
    return this.makeRequest({
      method: 'get',
      url: '/created',
      params,
    });
  }

  /**
   * Gets a schema from the ledger
   *
   * @param schemaId Schema identifier
   */
  get(schemaId: string): Observable<definitions['SchemaGetResult']> {
    return this.makeRequest({
      method: 'get',
      url: schemaId,
    });
  }

  /**
   * Writes a schema non-secret record to the wallet
   *
   * @param schemaId Schema identifier
   */
  writeRecord(schemaId: string): Observable<definitions['SchemaGetResult']> {
    return this.makeRequest({
      method: 'post',
      url: schemaId + '/write_record',
    });
  }
}
