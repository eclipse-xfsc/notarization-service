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

export interface RegisterNymParameters {
  /**
   * DID to register
   * @pattern ^(did:sov:)?[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{21,22}$
   * @example WgWxqztrNooG92RXvxSTWv
   */
  did: string;

  /**
   * Verification key
   * @pattern ^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{43,44}$
   * @example H3C2AVvLMv6gmMNam3uVAjZpfkcJCwDwnZn6z3wXmqPV
   */
  verkey: string;

  /**
   * Alias
   * @example Barry
   */
  alias?: string;

  /**
   * Connection identifier
   * @example 3fa85f64-5717-4562-b3fc-2c963f66afa6
   */
  connId?: string;

  /**
   * Create Transaction For Endorser's signature
   */
  createTransactionForEndorser?: boolean;

  /**
   * Role
   */
  role?: 'STEWARD' | 'TRUSTEE' | 'ENDORSER' | 'NETWORK_MONITOR' | 'reset';
}
