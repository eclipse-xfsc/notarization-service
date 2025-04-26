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
import { AbstractService } from '../abstract.service';
import { definitions } from '../types';
import { GetW3CCredentialsParameters } from './types/get-w3c-credentials-parameters.interface';

@Injectable()
export class CredentialsService extends AbstractService {
  protected readonly urlPrefix = 'credentials';

  /**
   * Fetch W3C credentials from wallet
   */
  getW3C(
    data: definitions['W3CCredentialsListRequest'],
    _params: GetW3CCredentialsParameters = {},
  ) {
    return this.makeRequest({
      method: 'post',
      url: 'w3c',
      data,
    });
  }
}
