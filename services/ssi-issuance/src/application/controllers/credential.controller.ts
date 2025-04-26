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
  Post,
  Query,
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { IssuanceService } from '../../domain/services/issuance.service';
import { VerificationService } from '../../domain/services/verification.service';
import { StartIssuanceHttpRequestBody } from '../dto/start-issuance.request.dto';
import {
  VerifyCredentialHttpRequestBody,
  VerifyCredentialHttpRequestQuery,
} from '../dto/verify-credential.request.dto';

@Controller('credential')
@UsePipes(
  new ValidationPipe({
    transform: true,
    whitelist: true,
    forbidNonWhitelisted: true,
  }),
)
export class CredentialController {
  constructor(
    private readonly issuanceService: IssuanceService,
    private readonly verificationService: VerificationService,
  ) {}

  @Post('start-issuance')
  @ApiTags('Credential issuance')
  @ApiOperation({
    summary: 'Start credential issuance',
    description: 'Start credential issuance process',
  })
  @ApiResponse({
    status: 201,
    description: 'The request for a credential issuance was accepted',
  })
  @ApiResponse({
    status: 400,
    description: 'Incoming data validation failed',
  })
  async startIssuance(@Body() body: StartIssuanceHttpRequestBody) {
    return this.issuanceService.start(body);
  }

  @Post('verify')
  @ApiTags('Credential verification')
  @ApiResponse({
    status: 201,
    description: 'The request for a credential verification was accepted',
  })
  @ApiResponse({
    status: 400,
    description: 'Incoming data validation failed',
  })
  async verify(
    @Body() body: VerifyCredentialHttpRequestBody,
    @Query() { successURL, failureURL }: VerifyCredentialHttpRequestQuery,
  ) {
    return this.verificationService.verify({ ...body, successURL, failureURL });
  }
}
