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

import { IsNotEmpty, IsOptional, IsString, IsUrl } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';
import { IsDID } from '../../infrastructure/utils/class-validator';

export class VerifyCredentialHttpRequestBody {
  @ApiProperty({
    description:
      'Identifier of the profile which defines the type of the credential, schema etc.',
    example: 'demo-vc-issuance-01-simple',
    examples: [
      'demo-vc-issuance-01-simple',
      'demo-vc-issuance-01-simple-portal',
      'demo-vc-issuance-01-identification-precondition',
      'demo-vc-issuance-01-without-tasks',
      'demo-vc-issuance-01-given-eIDAS-proof',
      'demo-vc-issuance-03-chaining-given-eIDAS',
      'demo-document-upload',
    ],
    format: 'string',
  })
  @IsString()
  @IsNotEmpty()
  readonly profileID: string;

  @ApiProperty({
    description:
      'DID of the party which is going to hold the credential being issued',
    example: 'did:key:z6Mktf9gwF31gNakSvYQVUpg3osGo9xqNtCTCEJcRp4tNGDg',
  })
  @IsString()
  @IsNotEmpty()
  @IsDID()
  readonly holderDID: string;

  @IsString()
  @IsNotEmpty()
  @IsUrl({ require_tld: false, allow_underscores: true })
  @ApiProperty({
    description: 'Invitation URL',
    format: 'string',
    example:
      'https://acapy-holder:8030?c_i=eyJAdHlwZSI6ICJkaWQ6c292OkJ6Q2JzTlloTXJqSGlxWkRUVUFTSGc7c3BlYy9jb25uZWN0aW9ucy8xLjAvaW52aXRhdGlvbiIsICJAaWQiOiAiNGNiNjhiZTUtNTI5Ni00ZjYwLTg0MjMtNTg4MmVlMzg4MmJiIiwgInJlY2lwaWVudEtleXMiOiBbIjNraml2VndnM1puaDhzM0ZIZHFyM2hZZmFGNkxyRlhBWFRtaHJXVzY1eWI4Il0sICJzZXJ2aWNlRW5kcG9pbnQiOiAiaHR0cDovL2FjYXB5LWhvbGRlcjo4MDMwIiwgImxhYmVsIjogIkhvbGRlciJ9',
  })
  readonly invitationURL: string;
}

export class VerifyCredentialHttpRequestQuery {
  @IsOptional()
  @IsString()
  @IsNotEmpty()
  @IsUrl({ require_tld: false, allow_underscores: true })
  @ApiProperty({
    description: 'The URL which will be called when the validation succeeds',
    format: 'string',
    example:
      'http://request-processing:8084/api/v1/finishtask/lZvRWQ==/success',
  })
  readonly successURL?: string;

  @IsOptional()
  @IsString()
  @IsNotEmpty()
  @IsUrl({ require_tld: false, allow_underscores: true })
  @ApiProperty({
    description: 'The URL which will be called when the validation fails',
    format: 'string',
    example: 'http://request-processing:8084/api/v1/finishtask/ELwbZw==/fail',
  })
  readonly failureURL?: string;
}
