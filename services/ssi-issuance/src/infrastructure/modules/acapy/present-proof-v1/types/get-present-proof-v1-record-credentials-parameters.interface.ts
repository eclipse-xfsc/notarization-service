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

export interface GetPresentProofV1RecordCredentialsParameters {
  /**
   * Maximum number to retrieve
   * @example 1
   */
  count: number;

  /**
   * (JSON) object mapping referents to extra WQL queries
   * @example {"0_drink_uuid": {"attr::drink::value": "martini"}}
   */
  extraQuery: string | Record<string, unknown>;

  /**
   * Proof request referents of interest
   * @example ["1_name_uuid", "2_score_uuid"]
   */
  referent: string[];

  /**
   * Start index
   * @example 1
   */
  start: number;
}
