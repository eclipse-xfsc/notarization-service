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
import { ConfigModule, ConfigService } from '@nestjs/config';
import { RouterModule } from '@nestjs/core';
import { FastifyReply, FastifyRequest } from 'fastify';
import { LoggerModule } from 'nestjs-pino';
import { CredentialController } from './application/controllers/credential.controller';
import { ProfileController } from './application/controllers/profile.controller';
import { StatusListCredentialController } from './application/controllers/status-list-credential.controller';
import { HealthModule } from './application/modules/health';
import config from './config';
import { AIP10ProfileService } from './domain/services/aip10-profile.service';
import { AIP20ProfileService } from './domain/services/aip20-profile.service';
import { ConnectionService } from './domain/services/connection.service';
import { IssuanceService } from './domain/services/issuance.service';
import { ProfileService } from './domain/services/profile.service';
import { StatusListCredentialsService } from './domain/services/status-list-credentials.service';
import { VerificationService } from './domain/services/verification.service';
import { AcapyModule } from './infrastructure/modules/acapy';
import { ProfileServiceModule } from './infrastructure/modules/profile-service';
import { RevocationServiceModule } from './infrastructure/modules/revocation-service';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      ignoreEnvFile: true,
      load: [config],
    }),

    LoggerModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        pinoHttp: {
          autoLogging: true,
          level: config.get('log.level'),
          formatters: {
            level: (level: string) => ({ level }),
          },
          serializers: {
            hostname: () => undefined,
            // pid: () => undefined,
            req: (req: FastifyRequest) => ({
              id: req.id,
              method: req.method,
              url: req.url,
              body: req.body,
            }),
            res: (res: FastifyReply) => ({
              status: res.statusCode,
            }),
          },
        },
      }),
    }),

    HealthModule,

    HttpModule.register({}),

    AcapyModule,
    ProfileServiceModule,
    RevocationServiceModule,

    RouterModule.register([{ path: 'healthz', module: HealthModule }]),
  ],
  controllers: [
    CredentialController,
    ProfileController,
    StatusListCredentialController,
  ],
  providers: [
    IssuanceService,
    VerificationService,
    ConnectionService,
    ProfileService,
    AIP10ProfileService,
    AIP20ProfileService,
    StatusListCredentialsService,
  ],
})
export class Application {}
