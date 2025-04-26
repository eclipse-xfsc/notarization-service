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

export interface GetAllDidsParameters {
  /**
   * DID of interest
   */
  did?: string;

  /**
   * Key type to query for.
   */
  keyType?: 'ed25519' | 'bls12381g2';

  /**
   * DID method to query for. e.g. sov to only fetch indy/sov DIDs
   */
  method?: 'key' | 'sov';

  /**
   * Whether DID is current public DID, posted to ledger but current public DID, or local to the wallet
   */
  posture?: 'public' | 'posted' | 'wallet_only';

  /**
   * Verification key of interest
   */
  verkey?: string;
}
