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

import {
  Body,
  Controller,
  Param,
  Post,
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import { ApiOperation, ApiParam, ApiTags } from '@nestjs/swagger';
import { StatusListCredentialsService } from '../../domain/services/status-list-credentials.service';
import { IssueStatusListCredentialBody } from '../dto/issue-status-list-credential.dto';

@Controller()
@UsePipes(
  new ValidationPipe({
    transform: true,
    whitelist: true,
    forbidNonWhitelisted: true,
  }),
)
export class StatusListCredentialController {
  constructor(private readonly service: StatusListCredentialsService) {}

  @Post('list-credential/:profileID/issue')
  @ApiTags('Status list credential')
  @ApiOperation({
    deprecated: true,
    description:
      'Use [status-list-credential](#/Status%20list%20credential/StatusListCredentialController_issueStatusListCredential) endpoint instead',
  })
  @ApiParam({
    name: 'profileID',
    examples: {
      'demo-vc-issuance-01-simple': {
        summary: 'Simple VC',
        description:
          'This credential is a minimal VC without chained proofs. As data this credential should contain something well known such as https://schema.org/Person.',
        value: 'demo-vc-issuance-01-simple',
      },
      'demo-vc-issuance-01-simple-portal': {
        value: 'demo-vc-issuance-01-simple-portal',
        summary: 'Simple Portal VC',
        description:
          'This credential is a minimal VC without chained proofs. As data this credential should contain something well known such as https://schema.org/Person.',
      },
      'demo-vc-issuance-01-identification-precondition': {
        value: 'demo-vc-issuance-01-identification-precondition',
        summary: 'Simple VC with both identification types',
        description:
          'This credential is a minimal VC without chained proofs. As data this credential should contain something well known such as https://schema.org/Person.',
      },
      'demo-vc-issuance-01-without-tasks': {
        value: 'demo-vc-issuance-01-without-tasks',
        summary: 'Simple Portal VC without Tasks',
        description:
          'This credential is a minimal VC without any tasks. As data this credential should contain something well known such as https://schema.org/Person.',
      },
      'demo-vc-issuance-01-given-eIDAS-proof': {
        value: 'demo-vc-issuance-01-given-eIDAS-proof',
        summary: 'VC with eIDAS Proof',
        description:
          'The VC must add the eIDAS Proof into the credential. There is no chained root VC. As we want an organizational credential, the VC subject should be https://schema.org/Organization.',
      },
      'demo-vc-issuance-03-chaining-given-eIDAS': {
        value: 'demo-vc-issuance-03-chaining-given-eIDAS',
        summary: 'VC with chained eIDAS based VC',
        description:
          'The VC will have a chained proof (according to Aries RfC 104) to the organisation credential. As it describes an employee, the https://schema.org/Person should be used.',
      },
      'demo-document-upload': {
        value: 'demo-document-upload',
        summary: 'Document Upload',
        description: 'Simple profile to upload documents.',
      },
    },
  })
  issueListCredential(
    @Param('profileID') profileID: string,
    @Body() body: IssueStatusListCredentialBody,
  ) {
    return this.issueStatusListCredential(profileID, body);
  }

  @Post('status-list-credential/:profileID/issue')
  @ApiTags('Status list credential')
  @ApiParam({
    name: 'profileID',
    examples: {
      'demo-vc-issuance-01-simple': {
        summary: 'Simple VC',
        description:
          'This credential is a minimal VC without chained proofs. As data this credential should contain something well known such as https://schema.org/Person.',
        value: 'demo-vc-issuance-01-simple',
      },
      'demo-vc-issuance-01-simple-portal': {
        value: 'demo-vc-issuance-01-simple-portal',
        summary: 'Simple Portal VC',
        description:
          'This credential is a minimal VC without chained proofs. As data this credential should contain something well known such as https://schema.org/Person.',
      },
      'demo-vc-issuance-01-identification-precondition': {
        value: 'demo-vc-issuance-01-identification-precondition',
        summary: 'Simple VC with both identification types',
        description:
          'This credential is a minimal VC without chained proofs. As data this credential should contain something well known such as https://schema.org/Person.',
      },
      'demo-vc-issuance-01-without-tasks': {
        value: 'demo-vc-issuance-01-without-tasks',
        summary: 'Simple Portal VC without Tasks',
        description:
          'This credential is a minimal VC without any tasks. As data this credential should contain something well known such as https://schema.org/Person.',
      },
      'demo-vc-issuance-01-given-eIDAS-proof': {
        value: 'demo-vc-issuance-01-given-eIDAS-proof',
        summary: 'VC with eIDAS Proof',
        description:
          'The VC must add the eIDAS Proof into the credential. There is no chained root VC. As we want an organizational credential, the VC subject should be https://schema.org/Organization.',
      },
      'demo-vc-issuance-03-chaining-given-eIDAS': {
        value: 'demo-vc-issuance-03-chaining-given-eIDAS',
        summary: 'VC with chained eIDAS based VC',
        description:
          'The VC will have a chained proof (according to Aries RfC 104) to the organisation credential. As it describes an employee, the https://schema.org/Person should be used.',
      },
      'demo-document-upload': {
        value: 'demo-document-upload',
        summary: 'Document Upload',
        description: 'Simple profile to upload documents.',
      },
    },
  })
  issueStatusListCredential(
    @Param('profileID') profileID: string,
    @Body() { id, subject }: IssueStatusListCredentialBody,
  ) {
    return this.service.issue(profileID, { listId: id, ...subject });
  }
}
