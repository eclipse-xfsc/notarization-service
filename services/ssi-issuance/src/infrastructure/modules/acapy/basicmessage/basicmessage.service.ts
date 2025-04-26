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
import { Observable, Subject } from 'rxjs';
import { AbstractService } from '../abstract.service';
import { definitions } from '../types';
import { BasicMessage } from './types/basic-message.interface';

@Injectable()
export class BasicMessageService extends AbstractService {
  protected readonly urlPrefix = 'connections';
  private readonly messages = new Subject<BasicMessage>();

  /**
   * Send a basic message to a connection
   */
  sendMessage(connectionId: string): Observable<definitions['SendMessage']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/send-message`,
    });
  }

  /**
   *
   */
  handleWebhook(message: BasicMessage): void {
    this.messages.next(message);
  }

  /**
   *
   */
  subscribe(handler: (value: BasicMessage) => void) {
    this.messages.subscribe(handler);
  }
}
