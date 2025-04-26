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

@Injectable()
export class AcapyService {
  constructor(
    readonly actionMenu: ActionMenuService,
    readonly basicmessage: BasicMessageService,
    readonly connections: ConnectionsService,
    readonly credentialDefinition: CredentialDefinitionService,
    readonly credentials: CredentialsService,
    readonly didexchange: DIDExchangeService,
    readonly introduction: IntroductionService,
    readonly issueCredential: IssueCredentialV1Service,
    readonly issueCredentialV2: IssueCredentialV2Service,
    readonly jsonld: JsonldService,
    readonly ledger: LedgerService,
    readonly presentProof: PresentProofV1Service,
    readonly presentProofV2: PresentProofV2Service,
    readonly resolver: ResolverService,
    readonly schema: SchemasService,
    readonly trustping: TrustpingService,
    readonly wallet: WalletService,
  ) {}
}
