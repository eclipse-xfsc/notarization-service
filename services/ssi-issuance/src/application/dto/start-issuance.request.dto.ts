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

import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import {
  IsArray,
  IsISO8601,
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  IsUrl,
  Validate,
  ValidateNested,
} from 'class-validator';
import {
  IsDID,
  ValidateNoGlobals,
} from '../../infrastructure/utils/class-validator';

class CredentialData {
  @ApiProperty({
    description: 'A set of claims to be added to the credential being issued',
  })
  @IsObject()
  @IsOptional()
  readonly credentialSubject?: Record<string, unknown>;

  @IsOptional()
  @IsArray()
  @IsObject({ each: true })
  readonly evidence?: Record<string, unknown>[];

  [k: string]: unknown;
}

export class StartIssuanceHttpRequestBody {
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
    example: 'did:key:z6Mkmi7fySZJfaqffB1m7HydwN4Leobbyi942sghAdwZa8mE',
  })
  @IsString()
  @IsNotEmpty()
  @IsDID()
  @IsOptional()
  readonly holderDID?: string;

  @ApiProperty({
    description: 'Issuance date of the credential being created',
    format: 'string',
    example: '2022-03-15T17:32:57.625Z',
  })
  @IsString()
  @IsNotEmpty()
  @IsISO8601()
  @IsOptional()
  readonly issuanceTimestamp?: string;

  @ApiProperty({
    description: 'Invitation URL',
    format: 'string',
    example:
      'http://acapy-holder:8030?c_i=eyJAdHlwZSI6ICJkaWQ6c292OkJ6Q2JzTlloTXJqSGlxWkRUVUFTSGc7c3BlYy9jb25uZWN0aW9ucy8xLjAvaW52aXRhdGlvbiIsICJAaWQiOiAiNGNiNjhiZTUtNTI5Ni00ZjYwLTg0MjMtNTg4MmVlMzg4MmJiIiwgInJlY2lwaWVudEtleXMiOiBbIjNraml2VndnM1puaDhzM0ZIZHFyM2hZZmFGNkxyRlhBWFRtaHJXVzY1eWI4Il0sICJzZXJ2aWNlRW5kcG9pbnQiOiAiaHR0cDovL2FjYXB5LWhvbGRlcjo4MDMwIiwgImxhYmVsIjogIkhvbGRlciJ9',
  })
  @IsString()
  @IsNotEmpty()
  @IsUrl({ require_tld: false, allow_underscores: true })
  @IsOptional()
  readonly invitationURL?: string;

  @ApiProperty({
    description: 'The contents data of the credential being issued',
    format: 'object',
    example: { credentialSubject: {}, evidence: [] },
  })
  @IsOptional()
  @Validate(ValidateNoGlobals)
  @Type(() => CredentialData)
  readonly credentialData?: CredentialData;

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
