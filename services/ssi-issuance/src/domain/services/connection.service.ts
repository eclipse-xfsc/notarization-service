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

import { Injectable, Logger } from '@nestjs/common';
import {
  filter,
  first,
  map,
  Observable,
  of,
  switchMap,
  throwError,
} from 'rxjs';
import { AcapyService, definitions } from '../../infrastructure/modules/acapy';
import {
  ConnectionRFC23State,
  ConnectionState,
} from '../../infrastructure/modules/acapy/connections';

@Injectable()
export class ConnectionService {
  private readonly logger = new Logger(ConnectionService.name);

  constructor(private readonly acapy: AcapyService) {}

  /**
   * The method returns either an existing connection for the recipient key
   * or tries to establish a new connection
   *
   * @param {String} invitationURL
   */
  fromInvitationURL(
    invitationURL: string,
  ): Observable<Required<definitions['ConnRecord']>> {
    this.logger.debug(
      { invitationURL },
      'Retrieving (or creating) a connection associated with the invitation',
    );

    // Decode the invitation
    const invitationBase64 = new URL(invitationURL).searchParams.get('c_i');

    if (typeof invitationBase64 !== 'string' || invitationBase64.length === 0) {
      this.logger.error({ invitationBase64 }, 'Invalid Base64 invitation');
      throw new Error(`Invalid Base64 invitation`);
    }

    const invitation = JSON.parse(
      Buffer.from(invitationBase64, 'base64').toString(),
    );

    return this.requestNewConnection(invitation) as Observable<
      Required<definitions['ConnRecord']>
    >;
  }

  fromID(
    connectionID: string,
  ): Observable<Required<definitions['ConnRecord']>> {
    const connection$ = this.acapy.connections.getConnection(connectionID);
    return connection$ as Observable<Required<definitions['ConnRecord']>>;
  }

  /**
   * Initiate a new connection from invitation
   *
   * @param invitation
   * @private
   */
  private requestNewConnection(
    invitation: definitions['ReceiveInvitationRequest'],
  ): Observable<definitions['ConnRecord']> {
    this.logger.debug({ invitation }, 'Create a new connection');

    const connection$ = this.acapy.connections
      .receiveInvitation(invitation, { autoAccept: true })
      .pipe(
        switchMap((connection) => {
          if (typeof connection?.connection_id === 'string') {
            if (connection.accept === 'manual') {
              return this.acapy.connections.acceptInvitation(
                connection.connection_id,
              );
            }
            return of(connection);
          }

          return throwError(() => {
            this.logger.error({ connection }, `Invalid connection record`);
            return new Error(`Invalid connection record`);
          });
        }),
      );

    return connection$;
  }

  private waitForReadiness(
    connection$: Observable<definitions['ConnRecord']>,
  ): Observable<definitions['ConnRecord']> {
    const predicate = ({ state, rfc23_state }: definitions['ConnRecord']) =>
      rfc23_state === ConnectionRFC23State.COMPLETED ||
      state === ConnectionState.COMPLETED;

    return connection$.pipe(
      switchMap((connection) =>
        this.acapy.connections.events.pipe(
          filter((conn) => conn.connection_id === connection.connection_id),
          first(predicate),
        ),
      ),
    );
  }

  resolveDidServices(DID: string) {
    return this.acapy.resolver.resolveDid(DID).pipe(
      map((didResolutionResult) => {
        const didDocument =
          didResolutionResult.did_doc ||
          (<any>didResolutionResult).did_document;

        if (
          !didDocument ||
          !Array.isArray(didDocument.service) ||
          didDocument.service.length === 0
        ) {
          return undefined;
        }

        return didDocument.service;
      }),
    );
  }
}
