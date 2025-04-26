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

import { HttpModule } from '@nestjs/axios';
import { Module } from '@nestjs/common';
import mapKeys from 'lodash/mapKeys';
import snakeCase from 'lodash/snakeCase';
import { AcapyConfigProvider } from './acapy-config.provider';
import { AcapyService } from './acapy.service';
import { ActionMenuService } from './action-menu';
import { BasicMessageService } from './basicmessage';
import { ConnectionsService } from './connections';
import { CredentialDefinitionService } from './credential-definition';
import { CredentialsService } from './credentials';
import { DIDExchangeService } from './did-exchange/did-exchange.service';
import { IntroductionService } from './introduction';
import { IssueCredentialV1Service } from './issue-credential-v1';
import { IssueCredentialV2Service } from './issue-credential-v2';
import { JsonldService } from './jsonld';
import { LedgerService } from './ledger';
import { PresentProofV1Service } from './present-proof-v1/present-proof-v1.service';
import { PresentProofV2Service } from './present-proof-v2';
import { ResolverService } from './resolver';
import { SchemasService } from './schema';
import { TrustpingService } from './trustping';
import { WalletService } from './wallet';
import { WebhooksController } from './webhooks.controller';

@Module({
  imports: [
    HttpModule.registerAsync({
      extraProviders: [AcapyConfigProvider],
      inject: [AcapyConfigProvider],
      useFactory: (config: AcapyConfigProvider) => {
        const baseURL = new URL(config.url);
        return {
          baseURL: baseURL.toString(),
          headers: { 'x-api-key': config.apiKey },
          paramsSerializer: {
            serialize: (params: Record<string, string>) =>
              new URLSearchParams(
                mapKeys(params, (_v, k) => snakeCase(k)),
              ).toString(),
          },
        };
      },
    }),
  ],
  controllers: [WebhooksController],
  providers: [
    AcapyService,
    AcapyConfigProvider,
    ActionMenuService,
    BasicMessageService,
    ConnectionsService,
    CredentialDefinitionService,
    CredentialsService,
    DIDExchangeService,
    IntroductionService,
    IssueCredentialV1Service,
    IssueCredentialV2Service,
    JsonldService,
    LedgerService,
    PresentProofV1Service,
    PresentProofV2Service,
    ResolverService,
    SchemasService,
    TrustpingService,
    WalletService,
  ],
  exports: [AcapyService],
})
export class AcapyModule {}
