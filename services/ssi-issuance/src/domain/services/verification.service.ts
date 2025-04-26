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
import { Injectable, Logger } from '@nestjs/common';
import { retryBackoff } from 'backoff-rxjs';
import { randomUUID } from 'crypto';
import jsonld from 'jsonld';
import {
  catchError,
  combineLatest,
  EMPTY,
  filter,
  first,
  from,
  map,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { AcapyService, definitions } from '../../infrastructure/modules/acapy';
import { PresentationExchangeRecordState } from '../../infrastructure/modules/acapy/present-proof-v2/types/pres-exchange-record-state.enum';
import { ProfileServiceAdapter } from '../../infrastructure/modules/profile-service';
import { Profile } from '../models/profile';
import { VerificationRequest } from '../models/verification-request';
import { VerifyRequest } from '../models/verify-request';
import { ConnectionService } from './connection.service';

@Injectable()
export class VerificationService {
  private readonly logger = new Logger(VerificationService.name);

  constructor(
    private readonly acapy: AcapyService,
    private readonly connectionService: ConnectionService,
    private readonly profile: ProfileServiceAdapter,
    private readonly httpService: HttpService,
  ) {}

  async verify({ invitationURL, profileID, ...request }: VerificationRequest) {
    const connection$ = this.connectionService.fromInvitationURL(invitationURL);
    const profile$ = this.profile.fetchProfile(profileID);

    const sendRequestPayload$ = combineLatest([connection$, profile$]).pipe(
      tap(() => this.logger.debug('Building the send request payload')),
      switchMap(([connection, profile]) =>
        from(this.createSendRequest({ connection, profile, ...request })),
      ),
    );

    sendRequestPayload$
      .pipe(
        tap((sendRequestPayload) => {
          this.logger.debug(
            { sendRequestPayload },
            'Sending the present proof v2 request',
          );
        }),

        switchMap((sendRequestPayload) =>
          this.acapy.presentProofV2.sendRequest(sendRequestPayload),
        ),

        tap(({ pres_ex_id }) =>
          this.logger.debug(
            { pres_ex_id },
            'Waiting for the presentation exchange record to be ready',
          ),
        ),

        switchMap(({ pres_ex_id }) =>
          this.acapy.presentProofV2.events.pipe(
            filter((presExRecord) => presExRecord.pres_ex_id === pres_ex_id),
            map((presExRecord) => {
              if (
                presExRecord.state === PresentationExchangeRecordState.ABANDONED
              ) {
                this.logger.error(
                  { presentationExchangeRecord: presExRecord },
                  `Presentation exchange record is in state '${presExRecord.state}'`,
                );
                throw new Error(
                  `Presentation exchange record is in state '${presExRecord.state}'`,
                );
              }
              return presExRecord;
            }),
            first(
              (presExRecord) =>
                presExRecord.state === PresentationExchangeRecordState.DONE,
            ),
            switchMap((presExRecord) =>
              this.acapy.presentProofV2.records.get(presExRecord.pres_ex_id),
            ),
          ),
        ),

        tap((presentationExRecord) => {
          if (typeof presentationExRecord.verified === 'undefined') {
            this.logger.error(
              { record: presentationExRecord },
              `Presentation exchange record is of unexpected format`,
            );
            throw new Error(
              `Presentation exchange record is of unexpected format`,
            );
          }

          if (presentationExRecord.verified === 'true') {
            this.logger.debug(`Credential verification was successful`);
            if (request.successURL) {
              this.notifyCaller(request.successURL);
            }
          } else {
            this.logger.debug(`Credential verification failed`);
            if (request.failureURL) {
              this.notifyCaller(request.failureURL);
            }
          }
        }),

        catchError((err) => {
          this.logger.error({ err }, `Credential verification failed`);
          if (request.failureURL) {
            this.notifyCaller(request.failureURL);
          }
          return EMPTY;
        }),
      )
      .subscribe();
  }

  private async createSendRequest({
    connection,
    holderDID,
    profile,
  }: VerifyRequest & { profile: Profile }): Promise<
    definitions['V20PresSendRequestRequest']
  > {
    const definitionID = randomUUID();
    const idFieldID = randomUUID();

    return {
      connection_id: connection.connection_id,
      auto_verify: true,
      auto_remove: false,
      presentation_request: {
        dif: {
          options: {},
          presentation_definition: {
            id: definitionID,
            format: {
              ldp_vp: { proofType: 'Ed25519Signature2018' },
            },
            input_descriptors: [
              {
                id: randomUUID(),
                constraints: {
                  fields: [
                    {
                      id: idFieldID,
                      path: ['credentialSubject.id'],
                      filter: {
                        const: holderDID,
                      },
                    },
                  ],
                  is_holder: [
                    {
                      field_id: [idFieldID],
                      directive: 'required',
                    },
                  ],
                },
                schema: await this.getSchemasFromProfile(profile),
              },
            ],
          },
        },
      },
    };
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private async getSchemasFromProfile({ template }: Profile): Promise<any> {
    const withExpandedTypes = await jsonld.expand(template);
    const expanded = withExpandedTypes[0];

    if (!expanded || !expanded['@type'] || !Array.isArray(expanded['@type'])) {
      this.logger.debug({ template }, 'Could not expand JSON-LD');
      throw new Error('Could not expand JSON-LD');
    }

    return expanded['@type']?.map((uri: string) => ({ required: true, uri }));
  }

  private notifyCaller(urlString: string) {
    let url: URL;

    of(urlString)
      .pipe(
        map((urlString) => (url = new URL(urlString))),
        tap((url) => {
          this.logger.debug(`Going to notify the caller on ${url.href}`);
        }),
        switchMap((url: URL) => this.httpService.post(url.href, {})),
        retryBackoff({
          initialInterval: 100,
          maxRetries: 2,
          resetOnSuccess: true,
        }),
        catchError((err) => {
          this.logger.error(
            { err },
            `Could not notify the caller on ${url.href}`,
          );
          return EMPTY;
        }),
        tap(() => {
          this.logger.debug(
            { url: url.href },
            `Successfully notified the called on ${url.href}`,
          );
        }),
      )
      .subscribe();
  }
}
