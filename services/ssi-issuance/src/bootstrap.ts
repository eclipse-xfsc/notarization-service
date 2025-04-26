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

/* istanbul ignore file */
import fastifyHelmet from '@fastify/helmet';
import { NestFactory } from '@nestjs/core';
import {
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
import { Logger } from 'nestjs-pino';
import { Application } from './application';
import { config } from './config.js';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const pkg = require('../package.json');

// Apply Duration plugin to DayJS
dayjs.extend(duration);

export const bootstrap = async () => {
  const app = await NestFactory.create<NestFastifyApplication>(
    Application,
    new FastifyAdapter({ ignoreTrailingSlash: true }),
    { bufferLogs: process.env.LOG_BUFFER_INITIAL === 'true' },
  );

  // See https://docs.nestjs.com/techniques/logger#injecting-a-custom-logger
  const logger = app.get(Logger);
  app.useLogger(logger);

  // See https://docs.nestjs.com/security/helmet#use-with-fastify
  await app.register(fastifyHelmet, {
    contentSecurityPolicy: {
      directives: {
        defaultSrc: [`'self'`],
        styleSrc: [`'self'`, `'unsafe-inline'`],
        imgSrc: [`'self'`, 'data:', 'validator.swagger.io'],
        scriptSrc: [`'self'`, `https: 'unsafe-inline'`],
      },
    },
  });

  // See https://docs.nestjs.com/fundamentals/lifecycle-events#application-shutdown
  app.enableShutdownHooks();

  // See https://docs.nestjs.com/openapi/introduction
  const swaggerModuleConfig = new DocumentBuilder()
    .setTitle(pkg.name)
    .setDescription(pkg.description)
    .setVersion(pkg.version)
    .build();
  const document = SwaggerModule.createDocument(app, swaggerModuleConfig);
  SwaggerModule.setup('api', app, document);

  const { host, port } = config.get('http');

  await app.listen(port, host);

  logger.log(`Service is running on ${await app.getUrl()}`);
};
