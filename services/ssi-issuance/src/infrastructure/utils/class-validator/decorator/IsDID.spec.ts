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

import { ValidationError, Validator } from 'class-validator';
import { IsDID } from './IsDID';

describe('IsDID', () => {
  const validator = new Validator();

  class ValidatedClass {
    @IsDID()
    did: string;
  }

  it('should successfully validate if the value is a valid DID', async () => {
    const model = new ValidatedClass();
    model.did = 'did:web:example.com';

    const errors = await validator.validate(model);

    expect(errors.length).toEqual(0);
  });

  it('should fail to validate if the value is not a valid DID', async () => {
    const model = new ValidatedClass();
    model.did = 'this:is:not:a:did';

    const errors = await validator.validate(model);

    expect(errors.length).toEqual(1);
    expect(errors[0]).toBeInstanceOf(ValidationError);
    expect(errors[0].constraints?.isDID).toEqual('did must be a DID');
  });
});
