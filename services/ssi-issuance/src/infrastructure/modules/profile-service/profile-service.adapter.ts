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

import { HttpService } from '@nestjs/axios';
import { Injectable, Logger } from '@nestjs/common';
import { plainToInstance } from 'class-transformer';
import { map, Observable, tap } from 'rxjs';
import { Profile } from '../../../domain/models/profile';
import { ProfileDIDs } from '../../../domain/models/profile-dids';
import {
  ProfileAIP10SSIData,
  ProfileAIP20SSIData,
} from '../../../domain/models/profile-ssi-data';

@Injectable()
export class ProfileServiceAdapter {
  private readonly logger = new Logger(ProfileServiceAdapter.name);

  constructor(protected readonly httpService: HttpService) {}

  fetchProfile(profileID: string): Observable<Profile> {
    this.logger.debug({ profileID }, 'Retrieving a profile');
    return this.httpService.get(`/api/v1/profiles/${profileID}`).pipe(
      tap((value) => {
        this.logger.debug(value);
      }),
      map(({ data }) => Profile.create(data)),
    );
  }

  getDIDs(profileID: string): Observable<ProfileDIDs> {
    this.logger.debug({ profileID }, 'Retrieving profile DIDs');
    return this.httpService
      .get(`/api/v1/profiles/${profileID}/ssi-data/v1`)
      .pipe(map(({ data }) => plainToInstance(ProfileDIDs, data)));
  }

  getSSIData<T extends ProfileAIP10SSIData | ProfileAIP20SSIData>(
    profileID: string,
  ): Observable<T> {
    this.logger.debug({ profileID }, 'Retrieving profile auxiliary data');
    return this.httpService.get(`/api/v1/profiles/${profileID}/ssi-data/v1`).pipe(
      map(({ data }) => {
        return data;
      }),
    );
  }
}
