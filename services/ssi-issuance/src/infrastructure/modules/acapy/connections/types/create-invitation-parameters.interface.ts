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

import { definitions } from '../../types';

export interface CreateInvitationParameters {
  body?: definitions['CreateInvitationRequest'];

  /**
   * Alias
   */
  alias?: string;

  /**
   * Auto-accept connection (defaults to configuration)
   */
  autoAccept?: boolean;

  /**
   * Create invitation for multiple use (default false)
   */
  multiUse?: boolean;

  /**
   * Create invitation from public DID (default false)
   */
  public?: boolean;
}
