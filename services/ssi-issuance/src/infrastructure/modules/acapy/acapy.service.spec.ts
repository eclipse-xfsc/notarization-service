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

import { AcapyService } from './';
import { ActionMenuService } from './action-menu';
import { BasicMessageService } from './basicmessage';
import { ConnectionsService } from './connections';
import { CredentialDefinitionService } from './credential-definition';
import { CredentialsService } from './credentials';
import { DIDExchangeService } from './did-exchange';
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

describe('AcapyService', () => {
  const actionMenuServiceMock = {} as ActionMenuService;
  const basicMessageServiceMock = {} as BasicMessageService;
  const connectionsServiceMock = {} as ConnectionsService;
  const credentialDefinitionServiceMock = {} as CredentialDefinitionService;
  const credentialsServiceMock = {} as CredentialsService;
  const didExchangeServiceMock = {} as DIDExchangeService;
  const introductionServiceMock = {} as IntroductionService;
  const issueCredentialServiceMock = {} as IssueCredentialV1Service;
  const issueCredentialV2ServiceMock = {} as IssueCredentialV2Service;
  const jsonldServiceMock = {} as JsonldService;
  const ledgerServiceMock = {} as LedgerService;
  const presentProofServiceMock = {} as PresentProofV1Service;
  const presentProofV2ServiceMock = {} as PresentProofV2Service;
  const resolverServiceMock = {} as ResolverService;
  const schemasServiceMock = {} as SchemasService;
  const trustpingServiceMock = {} as TrustpingService;
  const walletServiceMock = {} as WalletService;

  it('should instantiate an instance of the class', () => {
    const service = new AcapyService(
      actionMenuServiceMock,
      basicMessageServiceMock,
      connectionsServiceMock,
      credentialDefinitionServiceMock,
      credentialsServiceMock,
      didExchangeServiceMock,
      introductionServiceMock,
      issueCredentialServiceMock,
      issueCredentialV2ServiceMock,
      jsonldServiceMock,
      ledgerServiceMock,
      presentProofServiceMock,
      presentProofV2ServiceMock,
      resolverServiceMock,
      schemasServiceMock,
      trustpingServiceMock,
      walletServiceMock,
    );
    expect(service).toBeInstanceOf(AcapyService);
  });
});
