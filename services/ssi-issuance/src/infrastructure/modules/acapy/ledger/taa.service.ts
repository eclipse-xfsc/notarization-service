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

export class TaaService {
  constructor(private readonly makeRequest: AbstractService['makeRequest']) {}

  /**
   * Fetch the current transaction author agreement, if any
   */
  get(): Observable<definitions['TAAResult']> {
    return this.makeRequest({
      method: 'get',
      url: 'taa',
    });
  }

  /**
   * Accept the transaction author agreement
   */
  accept(
    data: definitions['TAAAccept'],
  ): Observable<definitions['LedgerModulesResult']> {
    return this.makeRequest({
      method: 'post',
      url: 'taa/accept',
      data,
    });
  }
}
