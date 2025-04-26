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
import convict from 'convict';
import 'dotenv/config';

export const schema = {
  env: {
    doc: 'Environment',
    format: ['production', 'development', 'test'],
    default: 'development',
    env: 'NODE_ENV',
  },

  log: {
    level: {
      doc: 'Log level',
      format: ['trace', 'debug', 'info', 'warn', 'error', 'fatal', 'silent'],
      default: 'warn',
      env: 'LOG_LEVEL',
    },

    bufferInitial: {
      doc: 'Buffer NestJS initial logs',
      format: Boolean,
      default: true,
      env: 'LOG_BUFFER_INITIAL',
    },
  },

  opentelemetry: {
    enabled: {
      doc: 'Controls if open telemetry is enabled',
      format: Boolean,
      default: false,
      env: 'OPENTELEMETRY_ENABLED',
    },
    logLevel: {
      doc: 'Log level for Open Telemetry',
      format: [
        'NONE',
        'none',
        'ERROR',
        'error',
        'WARN',
        'warn',
        'INFO',
        'info',
        'DEBUG',
        'debug',
        'VERBOSE',
        'verbose',
        'ALL',
        'all',
      ],
      default: 'NONE',
      env: 'OPENTELEMETRY_LOG_LEVEL',
    },
    traceExporterOTLPEndpoint: {
      doc: 'Endpoint to send telemetry to',
      format: String,
      default: '',
      env: 'OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT',
    },
  },

  http: {
    host: {
      doc: 'HTTP host to bind to',
      format: String,
      default: '0.0.0.0',
      env: 'HTTP_HOST',
    },
    port: {
      doc: 'HTTP port to listen on',
      format: 'port',
      default: 8180,
      env: 'HTTP_PORT',
    },
  },

  acapy: {
    url: {
      doc: 'URL of the Aries Cloud Agent (Python)',
      format: String,
      default: null,
      env: 'ACAPY_API_URL',
    },
    apiKey: {
      doc: 'API Key to communicate with ACA-Py',
      format: String,
      default: '',
      env: 'ACAPY_API_KEY',
    },
    webhooksPath: {
      doc: 'URL where ACA-Py will post webhooks. Only the pathname will be used. Hostname part is used for Docker Compose',
      format: String,
      default: '/acapy/webhooks',
      env: 'ACAPY_WEBHOOKS_PATH',
    },
    connections: {
      webhooks: {
        buffer: {
          doc: 'Number of webhook events to buffer',
          format: Number,
          default: 100,
          env: 'ACAPY_CONNECTIONS_WEBHOOKS_BUFFER',
        },
        timeWindow: {
          doc: 'The amount of time the buffered items will stay buffered (milliseconds)',
          format: Number,
          default: 60000,
          env: 'ACAPY_CONNECTIONS_WEBHOOKS_TIMEWINDOW',
        },
      },
    },
    issueCredentialV2: {
      webhooks: {
        buffer: {
          doc: 'Number of webhook events to buffer',
          format: Number,
          default: 100,
          env: 'ACAPY_ISSUE_CREDENTIAL_V2_WEBHOOKS_BUFFER',
        },
        timeWindow: {
          doc: 'The amount of time the buffered items will stay buffered (milliseconds)',
          format: Number,
          default: 60000,
          env: 'ACAPY_ISSUE_CREDENTIAL_V2_WEBHOOKS_TIMEWINDOW',
        },
      },
    },
    presentProofV2: {
      webhooks: {
        buffer: {
          doc: 'Number of webhook events to buffer',
          format: Number,
          default: 100,
          env: 'ACAPY_PRESENT_PROOF_V2_WEBHOOKS_BUFFER',
        },
        timeWindow: {
          doc: 'The amount of time the buffered items will stay buffered (milliseconds)',
          format: Number,
          default: 60000,
          env: 'ACAPY_PRESENT_PROOF_V2_WEBHOOKS_TIMEWINDOW',
        },
      },
    },
  },

  profileService: {
    url: {
      doc: 'URL of the Profile Service',
      format: String,
      default: null,
      env: 'PROFILE_SERVICE_URL',
    },
  },

  revocationService: {
    url: {
      doc: 'URL of the Revocation Service',
      format: String,
      default: null,
      env: 'REVOCATION_SERVICE_URL',
    },
  },
};

export const config = convict(schema).validate({ allowed: 'strict' });

export default () => config.get();
