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
  forkJoin,
  from,
  last,
  map,
  mergeMap,
  Observable,
  of,
  pluck,
  scan,
  shareReplay,
  switchMap,
} from 'rxjs';
import semver, { SemVer } from 'semver';
import { AcapyService, definitions } from '../../infrastructure/modules/acapy';
import { Profile } from '../models/profile';

@Injectable()
export class AIP10ProfileService {
  private readonly logger = new Logger(AIP10ProfileService.name);

  constructor(private readonly acapy: AcapyService) {}

  /**
   * This method initializes a profile which uses AIP (Aries Interop Protocols) 1.0
   * Namely, it creates a schema and a credential definition with the name of the profile
   */
  public initProfile(profile: Profile): Observable<unknown> {
    const did$ = this.getPublicDid();
    const schema$ = this.createSchema(profile);
    const credentialDefinition$ = this.createCredentialDefinition(schema$);

    return forkJoin({
      did: did$,
      schemaId: schema$.pipe(map(({ schema_id }) => schema_id)),
      credentialDefinitionId: credentialDefinition$.pipe(
        map(({ credential_definition_id }) => credential_definition_id),
      ),
    });
  }

  private getPublicDid() {
    return this.acapy.wallet.did
      .getPublicDid()
      .pipe(map(({ result }) => result?.did as string));
  }

  /**
   * This method tries to get the latest schema version,
   * or 0.0.0 in case schema was not yet created.
   * This is done by querying all schemas with the name of profile
   * and looking for maximal (in terms of [SemVer](https://semver.org/))
   * version number among them
   */
  private getNextSchemaVersion(schemaName: string) {
    // Determining the next version of the schema
    // If there are already schemas for the profile
    // the latest version will be taken and increased
    // I.e. 1.0.0 -> 2.0.0 -> 3.0.0 -> ...
    return this.acapy.schema.search({ schemaName }).pipe(
      switchMap(({ schema_ids }) => this.getLatestSchemaVersion(schema_ids)),
      map((version: string) => <string>semver.inc(version, 'major')),
      shareReplay(),
    );
  }

  /**
   * Creates a new schema
   */
  private createSchema(
    profile: Profile,
  ): Observable<definitions['SchemaSendResult']> {
    return this.getNextSchemaVersion(profile.id).pipe(
      switchMap((version) =>
        this.acapy.schema.sendSchema({
          schema_name: profile.id,
          schema_version: version,
          attributes: [...(<string[]>profile.template.attributes)],
        }),
      ),
      map((result) => {
        const { sent, schema, schema_id } = result;

        if (sent) {
          return sent as definitions['SchemaSendResult'];
        }

        if (schema_id && schema) {
          return { schema_id, schema } as definitions['SchemaSendResult'];
        }

        this.logger.error(
          { result },
          `Unexpected response for schema creation`,
        );
        throw new Error(`Unexpected response for schema creation`);
      }),
      shareReplay(),
    );
  }

  /**
   * Creates a credential definition
   */
  private createCredentialDefinition(
    schema$: Observable<definitions['SchemaSendResult']>,
  ): Observable<definitions['CredentialDefinitionSendResult']> {
    return schema$.pipe(
      switchMap(({ schema_id }) =>
        this.acapy.credentialDefinition.sendCredentialDefinition({
          schema_id,
        }),
      ),
      map((result) => {
        const { sent } = result;

        if (sent) {
          return sent as definitions['CredentialDefinitionSendResult'];
        }

        this.logger.error(
          { result },
          'Unexpected response for credential definition creation',
        );
        throw new Error(
          'Unexpected response for credential definition creation',
        );
      }),
    );
  }

  /**
   * This method tries to get the latest schema version,
   * or 0.0.0 in case schema was not yet created.
   * This is done by querying all schemas with the name of profile
   * and looking for maximal (in terms of [SemVer](https://semver.org/))
   * version number among them
   */
  private getLatestSchemaVersion(
    schemaIds?: Readonly<string[] | undefined>,
  ): Observable<string> {
    if (Array.isArray(schemaIds) && schemaIds.length > 0) {
      return from(schemaIds).pipe(
        // Request schemas for each of the schema IDs
        mergeMap((schemaId) => this.acapy.schema.get(schemaId)),
        // Get the version from each of the schema objects
        pluck<definitions['SchemaGetResult']>('schema', 'version'),
        // Find the max version so far
        scan<unknown, SemVer>((verA, b) => {
          const verB = semver.coerce(<string>b);
          return verB !== null && semver.gt(verB, verA) ? verB : verA;
        }, new SemVer('0.0.0')),
        // Return the max version
        last(),
        // Get the text representation of the version
        map(({ version }: SemVer) => <string>version),
      );
    }

    return of('0.0.0');
  }
}
