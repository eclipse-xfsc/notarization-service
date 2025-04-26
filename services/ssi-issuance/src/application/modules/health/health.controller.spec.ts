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

import { HealthCheckResult, HealthCheckService } from '@nestjs/terminus';
import { Test, TestingModule } from '@nestjs/testing';
import { HealthController } from './health.controller';

describe('HealthController', () => {
  const healthServiceMock = {
    check: jest.fn(),
  } as unknown as HealthCheckService;
  let controller: HealthController;

  beforeEach(async () => {
    const app: TestingModule = await Test.createTestingModule({
      controllers: [HealthController],
      providers: [{ provide: HealthCheckService, useValue: healthServiceMock }],
    }).compile();

    controller = app.get(HealthController);
  });

  it("should call the service's check method", async () => {
    const expectedResult = {} as HealthCheckResult;

    (healthServiceMock.check as jest.Mock).mockResolvedValue(expectedResult);

    const result = await controller.check();

    expect(result).toStrictEqual(expectedResult);
    expect(healthServiceMock.check).toBeCalledTimes(1);
    expect(healthServiceMock.check).toBeCalledWith([]);
  });
});
