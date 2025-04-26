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

import { HttpService } from '@nestjs/axios';
import {
  Injectable,
  Logger,
  OnModuleDestroy,
  OnModuleInit,
} from '@nestjs/common';
import { plainToInstance } from 'class-transformer';
import {
  catchError,
  defaultIfEmpty,
  defer,
  EMPTY,
  first,
  forkJoin,
  map,
  Observable,
  of,
  shareReplay,
  Subject,
  switchMap,
  takeUntil,
  tap,
  throwError,
} from 'rxjs';
import { AcapyService, definitions } from '../../infrastructure/modules/acapy';
import { ConnectionRFC23State } from '../../infrastructure/modules/acapy/connections';
import { ProfileServiceAdapter } from '../../infrastructure/modules/profile-service';
import { RevocationServiceAdapter } from '../../infrastructure/modules/revocation-service';
import { VerifiableCredentialContext } from '../enums/vc-context';
import { VerifiableCredentialProofType } from '../enums/vc-proof-type';
import { AIP } from '../models/aip';
import { Anoncred } from '../models/anoncred';
import { IssueRequest } from '../models/issue-request';
import {
  ProfileAIP10SSIData,
  ProfileAIP20SSIData,
} from '../models/profile-ssi-data';
import { StartIssuanceRequest } from '../models/start-issuance-request';
import { VerifiableCredential } from '../models/verifiable-credential';
import { ConnectionService } from './connection.service';

@Injectable()
export class IssuanceService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(IssuanceService.name);
  private readonly pendingRequests = new Map<string, StartIssuanceRequest>();
  private readonly unsubscribe$ = new Subject();

  constructor(
    private readonly acapy: AcapyService,
    private readonly connectionService: ConnectionService,
    private readonly profile: ProfileServiceAdapter,
    private readonly revocation: RevocationServiceAdapter,
    private readonly httpService: HttpService,
  ) {}

  /**
   * Listen for connection state changes, and proceed with issuance if
   * any of known connections changes its state to COMPLETED.
   * If a connection state changes to ABANDONED, remove the connection
   * from pending queue.
   * Ignore all other connection state changes.
   */
  onModuleInit() {
    this.acapy.connections.events
      .pipe(
        switchMap((entry) => {
          if (
            entry.rfc23_state === ConnectionRFC23State.COMPLETED &&
            this.pendingRequests.has(entry.connection_id)
          ) {
            const request = this.pendingRequests.get(
              entry.connection_id,
            ) as StartIssuanceRequest;

            request.holderDID =
              request.holderDID ||
              entry.their_public_did ||
              entry.their_did ||
              undefined;

            this.pendingRequests.delete(entry.connection_id);

            return this.proceed(entry.connection_id, request);
          }

          if (entry.state === ConnectionRFC23State.ABANDONED) {
            this.pendingRequests.delete(entry.connection_id);
          }

          return EMPTY;
        }),
        // Stop listening when something appears in `unsubscribe$` stream
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onModuleDestroy() {
    // Write to `unsubscribe$` stream to stop connection state change listener
    this.unsubscribe$.next(null);
    this.unsubscribe$.complete();
  }

  start(request: StartIssuanceRequest) {
    const invitationURL$ = new Subject();
    let connection$: Observable<Required<definitions['ConnRecord']>>;

    if (request.invitationURL) {
      connection$ = defer(() =>
        this.connectionService.fromInvitationURL(<string>request.invitationURL),
      );
      invitationURL$.complete();
    } else if (request.holderDID) {
      const holderDid = <string>request.holderDID;
      connection$ = defer(() =>
        this.connectionService.resolveDidServices(holderDid).pipe(
          switchMap((didServices) => {
            const isPublic =
              didServices &&
              didServices.some(({ type }) => type === 'did-communication');

            if (!isPublic) {
              this.logger.debug(
                `Creating invitation because no invitation URL was provided.`,
              );
              return this.acapy.connections
                .createInvitation({
                  autoAccept: true,
                })
                .pipe(
                  tap(({ invitation_url }) => {
                    this.logger.debug(
                      `Received invitation URL: ${invitation_url}.`,
                    );
                    invitationURL$.next(<string>invitation_url);
                    invitationURL$.complete();
                  }),
                  switchMap(({ connection_id }) =>
                    this.connectionService.fromID(<string>connection_id),
                  ),
                );
            } else {
              this.logger.debug(
                `Creating DIDExchange request for public holder DID: ${holderDid}.`,
              );
              const connection = this.acapy.didexchange.createRequest({
                their_public_did: holderDid,
              }) as Observable<Required<definitions['ConnRecord']>>;
              invitationURL$.complete();
              return connection;
            }
          }),
        ),
      );
    } else {
      connection$ = this.acapy.connections
        .createInvitation({
          autoAccept: true,
        })
        .pipe(
          tap(({ invitation_url }) => {
            invitationURL$.next(<string>invitation_url);
            invitationURL$.complete();
          }),
          switchMap(({ connection_id }) =>
            this.connectionService.fromID(<string>connection_id),
          ),
        );
    }

    connection$
      .pipe(
        tap(({ connection_id }) => {
          this.pendingRequests.set(connection_id, request);
        }),
      )
      .subscribe();

    return invitationURL$.pipe(
      defaultIfEmpty(null),
      map((invitationURL) => ({ invitationURL })),
    );
  }

  proceed(
    connectionId: string,
    { profileID, ...request }: StartIssuanceRequest,
  ) {
    return this.profile.fetchProfile(profileID).pipe(
      switchMap((profile) => {
        let holderDID: string | undefined;

        if (request.holderDID) {
          holderDID =
            request.holderDID.startsWith('did:sov:') ||
            request.holderDID.startsWith('did:key:')
              ? request.holderDID
              : `did:sov:${request.holderDID}`;
        }

        const issueRequest = plainToInstance(IssueRequest, {
          profile,
          connectionId,
          ...request,
          holderDID,
        });

        switch (profile.aip) {
          case AIP.AIP1:
            return this.issueAIP10(issueRequest);
          case AIP.AIP2:
            return this.issueAIP20(issueRequest);
          default:
            return throwError(
              () => new Error(`'${profile.aip}' is not supported`),
            );
        }
      }),
      switchMap(() => of(request.successURL)),
      catchError(() => of(request.failureURL)),
      first(),
      switchMap((callbackURL) => {
        if (callbackURL) {
          this.logger.debug(`Executing callback ${callbackURL}`);
          return this.httpService.post(callbackURL).pipe(
            tap(() => {
              this.logger.debug(
                `Successfully executed callback ${callbackURL}`,
              );
            }),
            catchError((err) => {
              this.logger.error(
                { err },
                `Failed to execute callback ${callbackURL}`,
              );
              return EMPTY;
            }),
          );
        } else {
          return EMPTY;
        }
      }),
    );
  }

  private issueAIP10({ profile, connectionId, credentialData }: IssueRequest) {
    this.logger.debug('Starting issuance of an AnonCred');

    return this.profile.getSSIData<ProfileAIP10SSIData>(profile.id).pipe(
      switchMap(({ did, schemaId, credentialDefinitionId }) => {
        const ac = Anoncred.fromTemplate(profile.template);

        ac.setConnectionId(connectionId)
          .setIssuer(did)
          .setSchemaId(schemaId)
          .setCredentialDefinitionId(credentialDefinitionId);

        // Just a check to make sure we received a non-empty object (not an array)
        if (
          typeof credentialData === 'object' &&
          !Array.isArray(credentialData) &&
          Object.keys(credentialData).length > 0
        ) {
          ac.addAttributes(credentialData);
        }

        return this.acapy.issueCredential.send(
          <definitions['V10CredentialProposalRequestMand']>ac.toPlain(),
        );
      }),
    );
  }

  private issueAIP20({
    connectionId,
    profile,
    holderDID,
    credentialData,
    issuanceTimestamp,
  }: IssueRequest) {
    this.logger.debug('Starting issuance of a Verifiable Credential');

    const issuerDID$ = this.profile
      .getSSIData<ProfileAIP20SSIData>(profile.id)
      .pipe(
        map(({ issuingDid }) => issuingDid),
        shareReplay(),
      );

    const credentialStatus$ = profile.isRevocable
      ? this.revocation.addStatusEntry(profile.id)
      : of(null);

    return forkJoin({
      issuerDID: issuerDID$,
      credentialStatus: credentialStatus$,
    }).pipe(
      switchMap(({ issuerDID, credentialStatus }) => {
        const credential = VerifiableCredential.from(
          credentialData || {},
          profile.template,
        );
        credential.setIssuer(issuerDID);
        credential.setHolder(holderDID);
        credential.setIssuanceDate(issuanceTimestamp);

        if (profile.validFor) {
          credential.setExpirationDate(profile.validFor);
        }

        if (credentialStatus) {
          credential.addContext(VerifiableCredentialContext.StatusList2021);
          credential.setStatus(credentialStatus);
        }

        return this.acapy.issueCredentialV2.send({
          connection_id: connectionId,
          filter: {
            ld_proof: {
              credential: credential.toPlain(),
              options: {
                proofType: VerifiableCredentialProofType.Ed25519Signature2018,
                credentialStatus: credential.status,
              },
            },
          },
        });
      }),
    );
  }
}
