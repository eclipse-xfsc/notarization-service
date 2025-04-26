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
  Logger,
  Param,
  Post,
  UsePipes,
  ValidationPipe,
} from '@nestjs/common';
import { IssueCredentialV2Service } from './issue-credential-v2';
import { PresentProofV2Service } from './present-proof-v2';
import { WebhookTopic } from './types/webhook-topic.enum';
import { ConnectionsService } from './connections';
import { ApiExcludeController } from '@nestjs/swagger';
import { BasicMessageService } from './basicmessage';
import { BasicMessage } from './basicmessage/types/basic-message.interface';
import { definitions } from './types';
import { PATH_METADATA } from '@nestjs/common/constants';
import { AcapyConfigProvider } from './acapy-config.provider';

@Controller()
@UsePipes(
  new ValidationPipe({
    transform: true,
    whitelist: true,
    forbidNonWhitelisted: true,
  }),
)
@ApiExcludeController()
export class WebhooksController {
  private readonly logger = new Logger(WebhooksController.name);

  constructor(
    private readonly basicmessages: BasicMessageService,
    private readonly connections: ConnectionsService,
    private readonly issueCredentialV20: IssueCredentialV2Service,
    private readonly presentProofV20: PresentProofV2Service,
    acapyConfig: AcapyConfigProvider,
  ) {
    // Dynamically define the webhooks endpoint
    const webhooksEndpoint = acapyConfig.webhooksEndpoint;
    Reflect.defineMetadata(PATH_METADATA, webhooksEndpoint, WebhooksController);
  }

  @Post('topic/:topic')
  async handleWebhook(
    @Param('topic') topic: WebhookTopic,
    @Body() body: unknown,
  ) {
    this.logger.verbose({ body }, 'Webhook received');

    switch (topic) {
      case WebhookTopic.BASIC_MESSAGES:
        return this.basicmessages.handleWebhook(body as BasicMessage);

      case WebhookTopic.CONNECTIONS:
        return this.connections.handleWebhook(
          body as Required<definitions['ConnRecord']>,
        );

      case WebhookTopic.ISSUE_CREDENTIAL_V20:
        return this.issueCredentialV20.handleWebhook(
          body as Required<definitions['V20CredExRecord']>,
        );

      case WebhookTopic.PRESENT_PROOF_V20:
        return this.presentProofV20.handleWebhook(
          body as Required<definitions['V20PresExRecord']>,
        );

      case WebhookTopic.FORWARD:
        throw new Error(`${topic} topic is not yet supported`);

      default:
      // Just doing nothing.
      // Controller should return 200 to not make ACA-Py anxious.
    }
  }
}
