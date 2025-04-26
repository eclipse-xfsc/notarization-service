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
import { Inject, Logger } from '@nestjs/common';
import { AxiosRequestConfig } from 'axios';
import { catchError, EMPTY, map, tap } from 'rxjs';
import { AcapyConfigProvider } from './acapy-config.provider';

export abstract class AbstractService {
  protected readonly urlPrefix: string;
  protected readonly logger: Logger;

  constructor(
    @Inject(HttpService) protected readonly httpService: HttpService,
    @Inject(AcapyConfigProvider) protected readonly config: AcapyConfigProvider,
  ) {
    this.logger = new Logger(this.constructor.name);
    this.logger.debug(this.constructor.name + ' initialized');
  }

  protected _baseURL: string;

  get baseURL() {
    if (!this._baseURL) {
      const baseURL = new URL(this.config.url);
      baseURL.pathname = this.urlPrefix;
      this._baseURL = baseURL.toString();
    }
    return this._baseURL;
  }

  /**
   *
   */
  protected makeRequest(requestConfig: AxiosRequestConfig) {
    return this.httpService
      .request({ ...requestConfig, baseURL: this.baseURL })
      .pipe(
        tap((result) => {
          this.logger.debug(result);
        }),
        map(({ data }) => data),
        catchError((error) => {
          this.logger.error(
            error,
            'Caught an error during a request to ACA-Py',
          );
          return EMPTY;
        }),
      );
  }
}
