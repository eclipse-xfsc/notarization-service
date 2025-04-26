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
export class ActionMenuService extends AbstractService {
  protected readonly urlPrefix = 'action-menu';

  /**
   * Close the active menu associated with a connection
   *
   * @param connectionId Connection identifier
   */
  close(
    connectionId: string,
  ): Observable<definitions['ActionMenuModulesResult']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/close`,
    });
  }

  /**
   * Fetch the active menu
   *
   * @param connectionId Connection identifier
   */
  fetch(
    connectionId: string,
  ): Observable<definitions['ActionMenuFetchResult']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/fetch`,
    });
  }

  /**
   * Perform an action associated with the active menu
   *
   * @param connectionId Connection identifier
   */
  perform(connectionId: string): Observable<definitions['PerformRequest']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/perform`,
    });
  }

  /**
   * Request the active menu
   *
   * @param connectionId Connection identifier
   */
  request(
    connectionId: string,
    data: definitions['PerformRequest'],
  ): Observable<definitions['ActionMenuModulesResult']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/request`,
      data,
    });
  }

  /**
   * Send an action menu to a connection
   *
   * @param connectionId Connection identifier
   */
  sendMenu(
    connectionId: string,
  ): Observable<definitions['ActionMenuModulesResult']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/send-menu`,
    });
  }
}
