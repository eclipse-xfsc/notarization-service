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

import { diag, DiagConsoleLogger, DiagLogLevel } from '@opentelemetry/api';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { FastifyInstrumentation } from '@opentelemetry/instrumentation-fastify';
import { HttpInstrumentation } from '@opentelemetry/instrumentation-http';
import { NestInstrumentation } from '@opentelemetry/instrumentation-nestjs-core';
import { Resource } from '@opentelemetry/resources';
import {
  BasicTracerProvider,
  BatchSpanProcessor,
  ConsoleSpanExporter,
  SimpleSpanProcessor,
} from '@opentelemetry/sdk-trace-base';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';
import { config } from './config.js';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const pkg = require('../package.json');

(async () => {
  if (config.get('opentelemetry.enabled') === true) {
    const logLevel =
      DiagLogLevel[
        (
          config.get('opentelemetry.logLevel') || 'none'
        ).toUpperCase() as keyof typeof DiagLogLevel
      ];

    diag.setLogger(new DiagConsoleLogger(), logLevel);

    const exporter = new OTLPTraceExporter({
      url: config.get('opentelemetry.traceExporterOTLPEndpoint') as string,
    });

    const tracerProvider = new BasicTracerProvider({
      resource: new Resource({
        [SemanticResourceAttributes.SERVICE_NAME]: pkg.name,
        [SemanticResourceAttributes.SERVICE_VERSION]: pkg.version,
      }),
    });

    tracerProvider.addSpanProcessor(new BatchSpanProcessor(exporter));

    // logging for debug
    if (logLevel >= DiagLogLevel.DEBUG) {
      tracerProvider.addSpanProcessor(
        new SimpleSpanProcessor(new ConsoleSpanExporter()),
      );
    }

    tracerProvider.register();

    registerInstrumentations({
      tracerProvider,
      instrumentations: [
        new NestInstrumentation(),
        new HttpInstrumentation(),
        new FastifyInstrumentation(),
      ],
    });
  }

  const { bootstrap } = await import('./bootstrap.js');

  return bootstrap();
})();
