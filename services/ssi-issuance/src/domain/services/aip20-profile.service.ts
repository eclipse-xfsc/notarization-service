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

import { Injectable, Logger } from '@nestjs/common';
import { map, mergeMap, Observable, of, toArray } from 'rxjs';
import { AcapyService, definitions } from '../../infrastructure/modules/acapy';
import { Profile } from '../models/profile';

@Injectable()
export class AIP20ProfileService {
  private readonly logger = new Logger(AIP20ProfileService.name);

  constructor(private readonly acapy: AcapyService) {}

  /**
   * This method initialized an AIP 2.0 profile.
   * Namely, it creates two DIDs: for issuing and for revocation
   */
  public initProfile(_profile: Profile): Observable<unknown> {
    const didCreatePayload: definitions['DIDCreate'] = {
      method: 'key',
      options: { key_type: 'ed25519' },
    };

    return of(1, 2).pipe(
      mergeMap(() => this.acapy.wallet.did.create(didCreatePayload)),
      map(({ result }) => {
        if (!result?.did) {
          this.logger.debug({ result }, 'Could not create a new DID');
          throw new Error('Could not create a new DID');
        }
        return result.did;
      }),
      toArray(),
      map(([issuingDid, revocatingDid]) => ({ issuingDid, revocatingDid })),
    );
  }
}
