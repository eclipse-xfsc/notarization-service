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
  FastifyAdapter,
  NestFastifyApplication,
} from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import { Application } from '../../../src/application';

describe('Start credential issuance', () => {
  let app: NestFastifyApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [Application],
    }).compile();

    const fastifyAdapter = new FastifyAdapter();

    app =
      moduleRef.createNestApplication<NestFastifyApplication>(fastifyAdapter);
  });

  afterAll(async () => {
    await app.close();
  });

  describe('Start credential issuance', () => {
    it.todo('should successfully start a credential issuance process');
  });
});
