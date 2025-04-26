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

@Injectable()
export class JsonldService extends AbstractService {
  protected readonly urlPrefix = 'jsonld';

  /**
   * Sign a JSON-LD structure and return it
   */
  sign(
    data: definitions['SignRequest'],
  ): Observable<definitions['SignResponse']> {
    return this.makeRequest({
      method: 'post',
      url: 'sign',
      data,
    });
  }

  /**
   * Verify a JSON-LD structure
   */
  verify(
    data: definitions['VerifyRequest'],
  ): Observable<definitions['VerifyResponse']> {
    return this.makeRequest({
      method: 'post',
      url: 'verify',
      data,
    });
  }
}
