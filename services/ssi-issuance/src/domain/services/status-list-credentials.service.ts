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

import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { AxiosError } from 'axios';
import {
  catchError,
  forkJoin,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';
import { AcapyService } from '../../infrastructure/modules/acapy';
import { ProfileServiceAdapter } from '../../infrastructure/modules/profile-service';
import { AIP } from '../models/aip';
import { IssueStatusListCredentialRequest } from '../models/issue-status-list-credential-request';
import { Profile } from '../models/profile';
import { ProfileDIDs } from '../models/profile-dids';

@Injectable()
export class StatusListCredentialsService {
  private readonly logger = new Logger(StatusListCredentialsService.name);

  constructor(
    private readonly profileService: ProfileServiceAdapter,
    private readonly acapy: AcapyService,
  ) {}

  issue(profileID: string, requestData: IssueStatusListCredentialRequest) {
    this.logger.debug(
      { profileID, ...requestData },
      'Starting issuance of a status list credential',
    );

    return this.profileService.fetchProfile(profileID).pipe(
      switchMap((profile) => {
        switch (profile.aip) {
          case AIP.AIP1:
            return of({});
          case AIP.AIP2:
            return this.issueAIP20(profile, requestData);
          default:
            this.logger.error(
              { profile },
              `Profile AIP is not specified or not supported`,
            );
            return of({});
        }
      }),
    );
  }

  private issueAIP20(
    profile: Profile,
    requestData: IssueStatusListCredentialRequest,
  ) {
    const profileDIDs$ = this.profileService.getDIDs(profile.id).pipe(
      catchError((err) => {
        if (
          err.isAxiosError &&
          err.code === AxiosError.ERR_BAD_REQUEST &&
          err.response?.status === 404
        ) {
          this.logger.error({ err });
          return throwError(
            () =>
              new NotFoundException(
                `Could not find profile '${profile.id}' dids`,
              ),
          );
        }
        return throwError(err);
      }),
      shareReplay(),
    );

    const verkey$ = profileDIDs$.pipe(
      tap(({ issuingDid }) =>
        this.logger.debug(`xGetting verification key for DID ${issuingDid}`),
      ),
      switchMap(({ issuingDid }) =>
        this.acapy.wallet.did.getAll({ did: issuingDid }),
      ),
      map(({ results }) => {
        if (
          typeof results === 'undefined' ||
          results.length === 0 ||
          typeof results[0].verkey !== 'string'
        ) {
          throw new Error(`Could not get the verification key for DID`);
        }
        return results[0].verkey;
      }),
    );

    return forkJoin({
      profileDIDs: profileDIDs$,
      verkey: verkey$,
    }).pipe(
      switchMap(({ profileDIDs, verkey }) => {
        // TODO: Think about having a concrete class to create Verifiable Credential data
        const credentialData = this.prepareCredentialData(
          profile,
          profileDIDs,
          requestData,
        );

        return this.acapy.jsonld.sign({
          doc: {
            credential: credentialData,
            options: {
              proofPurpose: 'assertionMethod',
              verificationMethod: credentialData.issuer,
            },
          },
          verkey,
        });
      }),
      map(({ signed_doc }) => signed_doc),
    );
  }

  private prepareCredentialData(
    _profile: Profile,
    profileDIDs: ProfileDIDs,
    requestData: IssueStatusListCredentialRequest,
  ) {
    return {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://w3id.org/vc/status-list/2021/v1',
      ],
      '@type': ['VerifiableCredential', 'StatusList2021Credential'],
      id: requestData.listId,
      issuer: profileDIDs.revocatingDid,
      issuanceDate: new Date(),
      credentialSubject: {
        id: requestData.id,
        '@type': requestData.type,
        statusPurpose: requestData.statusPurpose,
        encodedList: requestData.encodedList,
      },
    };
  }
}
