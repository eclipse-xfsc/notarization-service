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

export interface CreateStaticConnectionParameters {
  /**
   * Alias to assign to this connection
   */
  alias?: string;

  /**
   * Local DID
   * @pattern ^(did:sov:)?[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{21,22}$
   * @example WgWxqztrNooG92RXvxSTWv
   */
  myDid?: string;

  /**
   * Seed to use for the local DID
   */
  mySeed?: string;

  /**
   * Remote DID
   * @pattern ^(did:sov:)?[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{21,22}$
   * @example WgWxqztrNooG92RXvxSTWv
   */
  theirDid?: string;

  /**
   * URL endpoint for other party
   * @pattern ^[A-Za-z0-9\.\-\+]+://([A-Za-z0-9][.A-Za-z0-9-_]+[A-Za-z0-9])+(:[1-9][0-9]*)?(/[^?&#]+)?$
   * @example https://myhost:8021
   */
  theirEndpoint?: string;

  /**
   * Other party's label for this connection
   */
  theirLabel?: string;

  /**
   * Seed to use for the remote DID
   */
  theirSeed?: string;

  /**
   * Remote verification key
   */
  theirVerkey?: string;
}
