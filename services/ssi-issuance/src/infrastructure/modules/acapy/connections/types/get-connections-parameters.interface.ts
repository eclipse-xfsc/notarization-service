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

import { ConnectionProtocol } from './connection-protocol.enum';

export interface GetConnectionsParameters {
  /**
   * Alias
   */
  alias?: string;

  /**
   * Connection protocol used
   */
  connectionProtocol?: ConnectionProtocol;

  /**
   * Invitation key
   */
  invitationKey?: string;

  /**
   * My DID
   */
  myDid?: string;

  /**
   * Connection state
   */
  state?:
    | 'response'
    | 'init'
    | 'request'
    | 'invitation'
    | 'active'
    | 'start'
    | 'error'
    | 'completed'
    | 'abandoned';

  /**
   * Their DID
   */
  theirDid?: string;

  /**
   * Their role in the connection protocol
   */
  theirRole?: 'invitee' | 'requester' | 'inviter' | 'responder';
}
