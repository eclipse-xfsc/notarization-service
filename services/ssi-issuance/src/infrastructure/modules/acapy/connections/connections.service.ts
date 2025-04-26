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

import { Injectable } from '@nestjs/common';
import { mapKeys, snakeCase } from 'lodash';
import { Observable, ReplaySubject } from 'rxjs';
import { AbstractService } from '../abstract.service';
import { definitions } from '../types';
import { AcceptInvitationParameters } from './types/accept-invitation-parameters.interface';
import { AcceptRequestParameters } from './types/accept-request-parameters.interface';
import { CreateInvitationParameters } from './types/create-invitation-parameters.interface';
import { CreateStaticConnectionParameters } from './types/create-static-connection-parameters.interface';
import { GetConnectionsParameters } from './types/get-connections-parameters.interface';
import { ReceiveInvitationParameters } from './types/receive-invitation-parameters.interface';

@Injectable()
export class ConnectionsService extends AbstractService {
  protected readonly urlPrefix = 'connections';

  protected readonly connections = new ReplaySubject<
    Required<definitions['ConnRecord']>
  >(
    this.config.connections.webhooks.buffer,
    this.config.connections.webhooks.timeWindow,
  );

  /**
   * Query agent-to-agent connections
   */
  get(
    params: GetConnectionsParameters,
  ): Observable<definitions['ConnectionList']> {
    return this.makeRequest({ method: 'get', url: '', params });
  }

  /**
   * Create a new connection invitation
   */
  createInvitation(
    params: CreateInvitationParameters,
  ): Observable<definitions['InvitationResult']> {
    return this.makeRequest({
      method: 'post',
      url: 'create-invitation',
      params,
    });
  }

  /**
   * Create a new static connection
   */
  createStatic(
    params: CreateStaticConnectionParameters,
  ): Observable<definitions['ConnectionStaticRequest']> {
    return this.makeRequest({
      method: 'post',
      url: 'create-static',
      data: mapKeys(params, (_v, k) => snakeCase(k)),
    });
  }

  /**
   * Receive a new connection invitation
   */
  receiveInvitation(
    invitation: definitions['ReceiveInvitationRequest'],
    params: ReceiveInvitationParameters = {},
  ): Observable<definitions['ConnRecord']> {
    return this.makeRequest({
      method: 'post',
      url: 'receive-invitation',
      data: invitation,
      params,
    });
  }

  /**
   * Fetch a single connection record
   */
  getConnection(
    connectionId: string,
  ): Observable<Required<definitions['ConnRecord']>> {
    return this.makeRequest({ method: 'get', url: connectionId });
  }

  /**
   * Remove an existing connection record
   */
  deleteConnection(
    connectionId: string,
  ): Observable<definitions['ConnectionModuleResponse']> {
    return this.makeRequest({ method: 'delete', url: connectionId });
  }

  /**
   * Accept a stored connection invitation
   */
  acceptInvitation(
    connectionId: string,
    params: AcceptInvitationParameters = {},
  ): Observable<Required<definitions['ConnRecord']>> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/accept-invitation`,
      params,
    });
  }

  /**
   * Accept a stored connection request
   */
  acceptRequest(
    connectionId: string,
    params: AcceptRequestParameters = {},
  ): Observable<Required<definitions['ConnRecord']>> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/accept-request`,
      params,
    });
  }

  /**
   * Fetch connection remote endpoint
   */
  getConnectionEndpoints(
    connectionId: string,
  ): Observable<definitions['EndpointsResult']> {
    return this.makeRequest({
      method: 'get',
      url: `${connectionId}/endpoints`,
    });
  }

  /**
   * Assign another connection as the inbound connection
   *
   * @param connectionId Connection identifier
   * @param refId Inbound connection identifier
   */
  establishInbound(
    connectionId: string,
    refId: string,
  ): Observable<definitions['ConnectionModuleResponse']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/establish-inbound/${refId}`,
    });
  }

  /**
   * Fetch connection metadata
   * @param connectionId Connection identifier
   * @param key Key to retrieve
   */
  getMetadata(
    connectionId: string,
    key?: string,
  ): Observable<definitions['ConnectionMetadata']> {
    return this.makeRequest({
      method: 'get',
      url: `${connectionId}/metadata`,
      params: { key },
    });
  }

  /**
   * Set connection metadata
   * @param connectionId Connection identifier
   * @param metadata
   */
  setMetadata(
    connectionId: string,
    metadata: definitions['ConnectionMetadataSetRequest']['metadata'],
  ): Observable<definitions['ConnectionMetadata']> {
    return this.makeRequest({
      method: 'post',
      url: `${connectionId}/metadata`,
      data: { metadata },
    });
  }

  /**
   *
   */
  handleWebhook(connection: Required<definitions['ConnRecord']>) {
    this.connections.next(connection);
  }

  get events(): Observable<Required<definitions['ConnRecord']>> {
    return this.connections;
  }
}
