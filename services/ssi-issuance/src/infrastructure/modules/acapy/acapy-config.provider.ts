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

import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

interface WebhooksConfig {
  webhooks: {
    buffer: number;
    timeWindow: number;
  };
}

@Injectable()
export class AcapyConfigProvider {
  readonly url: string;
  readonly apiKey: string;
  readonly webhooksEndpoint: string;
  readonly connections: WebhooksConfig;
  readonly presentProofV2: WebhooksConfig;
  readonly issueCredentialV2: WebhooksConfig;

  constructor(configService: ConfigService) {
    const config = configService.get('acapy');

    this.url = config.url;
    this.apiKey = config.apiKey;
    this.connections = config.connections;
    this.presentProofV2 = config.presentProofV2;
    this.issueCredentialV2 = config.issueCredentialV2;
    this.webhooksEndpoint = config.webhooksPath;
  }
}
