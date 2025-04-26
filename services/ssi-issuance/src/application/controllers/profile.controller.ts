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
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { Observable } from 'rxjs';
import { ProfileService } from '../../domain/services/profile.service';
import { ProfileInitBody } from '../dto/profile-init.dto';

@Controller('profile')
@UsePipes(
  new ValidationPipe({
    transform: true,
    whitelist: true,
    forbidNonWhitelisted: true,
  }),
)
export class ProfileController {
  constructor(private readonly profileService: ProfileService) {}

  @Post('init')
  @ApiTags('Profile initialization')
  @ApiOperation({
    summary: 'Initialize a profile',
    description:
      'Initialize the process of a profile initialize. I.e. create a new DID for a profile.',
  })
  @ApiResponse({
    status: 201,
    description: 'A set of DIDs was successfully created',
    schema: {
      oneOf: [
        {
          type: 'object',
          properties: {
            did: {
              type: 'string',
              example: 'WgWxqztrNooG92RXvxSTWv',
            },
            schemaId: {
              type: 'string',
              example: 'WgWxqztrNooG92RXvxSTWv:2:schema_name:1.0',
            },
            credentialDefinitionId: {
              type: 'string',
              example: 'WgWxqztrNooG92RXvxSTWv:3:CL:20:tag',
            },
          },
        },
        {
          type: 'object',
          properties: {
            issuingDID: {
              type: 'string',
              example:
                'did:key:z6Mkk6hAC2EKXiXnvcdm43skvJY3A22ftaTEJo4F8L4MvG2Z',
            },
            revocatingDid: {
              type: 'string',
              example:
                'did:key:z6MkvjbKPx6QjgjgZYqYKdDAPv8NWgsZ3procJW6bBnkXkfn',
            },
          },
        },
      ],
    },
  })
  initProfile(@Body() body: ProfileInitBody): Observable<string> {
    return <Observable<string>>this.profileService.initProfile(body.profileID);
  }
}
