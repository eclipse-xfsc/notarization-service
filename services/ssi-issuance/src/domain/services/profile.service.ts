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

import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { AxiosError } from 'axios';
import { catchError, EMPTY, Observable, switchMap, throwError } from 'rxjs';
import { ProfileServiceAdapter } from '../../infrastructure/modules/profile-service';
import { AIP } from '../models/aip';
import { AIP10ProfileService } from './aip10-profile.service';
import { AIP20ProfileService } from './aip20-profile.service';

@Injectable()
export class ProfileService {
  private readonly logger = new Logger(ProfileService.name);

  constructor(
    private readonly profileService: ProfileServiceAdapter,
    private readonly aip10ProfileService: AIP10ProfileService,
    private readonly aip20ProfileService: AIP20ProfileService,
  ) {}

  initProfile(profileID: string): Observable<unknown> {
    return this.profileService.fetchProfile(profileID).pipe(
      catchError((err) => {
        if (err instanceof AxiosError && err.response?.status === 404) {
          return throwError(
            new NotFoundException(`Profile<${profileID}> was not found`),
          );
        }
        this.logger.error({ err });
        return throwError(err);
      }),
      switchMap((profile) => {
        if (profile.kind === "AnonCred") {
          return this.aip10ProfileService.initProfile(profile);
        }

        if (profile.kind === "JSON-LD") {
          return this.aip20ProfileService.initProfile(profile);
        }

        if (profile.kind === "SD-JWT") {
            return this.aip20ProfileService.initProfile(profile);
        }

        this.logger.error(
          { profile },
          `Profile Kind is not specified or not supported`,
        );
        return EMPTY;
      }),
    );
  }
}
