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

import { Type } from 'class-transformer';
import {
  Equals,
  IsNotEmpty,
  IsString,
  IsUrl,
  ValidateNested,
} from 'class-validator';

export class IssueStatusListCredentialSubject {
  @IsString()
  @IsNotEmpty()
  id: string = 'https://example.com/status/3#list';

  @IsString()
  @IsNotEmpty()
  @Equals('StatusList2021', {
    message: `Only 'StatusList2021' type is supported`,
  })
  type: string = 'StatusList2021';

  @IsString()
  @IsNotEmpty()
  @Equals('revocation', { message: `Only 'revocation' purpose is supported` })
  statusPurpose: string = 'revocation';

  @IsString()
  @IsNotEmpty()
  encodedList: string =
    'H4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA';
}

export class IssueStatusListCredentialBody {
  @IsString()
  @IsNotEmpty()
  @IsUrl({ require_tld: false })
  id: string = 'https://example.com/credentials/status/3';

  @ValidateNested()
  @Type(() => IssueStatusListCredentialSubject)
  subject: IssueStatusListCredentialSubject;
}
