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
import { AxiosError } from 'axios';
import { catchError, map, Observable, of, switchMap } from 'rxjs';
import { CredentialStatus } from '../../../domain/types/credential-status';

const PROFILE_LIST_EXISTS = Symbol();

@Injectable()
export class RevocationServiceAdapter {
  private readonly logger = new Logger(RevocationServiceAdapter.name);

  constructor(protected readonly httpService: HttpService) {}

  registerList(profileID: string) {
    this.logger.debug({ profileID }, 'Registering a list for profile');
    return this.httpService.post(
      '/management/lists',
      {},
      {
        params: { profile: profileID },
      },
    );
  }

  getList(profileID: string) {
    this.logger.debug({ profileID }, 'Retrieving the list for profile');
    return this.httpService.get(`/management/lists/${profileID}`);
  }

  addStatusEntry(profileID: string): Observable<CredentialStatus> {
    this.logger.debug(
      { profileID },
      'Making sure a list exists for the profile',
    );

    return this.getList(profileID).pipe(
      catchError((err) => {
        if (
          err instanceof AxiosError &&
          err.code === 'ERR_BAD_REQUEST' &&
          err.response?.status === 404
        ) {
          return this.registerList(profileID);
        }
        return of(PROFILE_LIST_EXISTS);
      }),
      switchMap(() => {
        this.logger.debug({ profileID }, 'Adding a status entry');
        return this.httpService.post(`/management/lists/${profileID}/entry`);
      }),
      map(({ data }) => data),
    );
  }
}
