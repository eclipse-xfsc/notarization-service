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

import { buildMessage, ValidateBy, ValidationOptions } from 'class-validator';

export const IS_DID = 'isDID';

// See https://www.w3.org/TR/did-core/#did-syntax
const RE_DID =
  /^did:[A-Za-z0-9]+(:([-_.A-Za-z0-9]|%[A-Fa-f0-9][A-Fa-f0-9])+)+$/;

export function isDID(value: unknown): boolean {
  return typeof value === 'string' && RE_DID.test(value);
}

export function IsDID(
  validationOptions?: ValidationOptions,
): PropertyDecorator {
  return ValidateBy(
    {
      name: IS_DID,
      constraints: [],
      validator: {
        validate: (value): boolean => isDID(value),
        defaultMessage: buildMessage(
          (eachPrefix) => eachPrefix + '$property must be a DID',
          validationOptions,
        ),
      },
    },
    validationOptions,
  );
}
