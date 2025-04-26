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

import { plainToInstance } from 'class-transformer';
import { AIP } from './aip';
import { NotaryAccess } from './notary-access';
import { ProfileTaskTree } from './profile-task-tree';
import { TaskDescription } from './task-description';

export class Profile {
  readonly id: string;
  readonly aip: AIP;
  readonly name: string;
  readonly kind: string;
  readonly description: string;
  readonly encryption: string;
  readonly notaries: NotaryAccess[];
  readonly validFor: string;
  readonly template: Record<string, unknown>;
  readonly taskDescriptions: TaskDescription[];
  readonly tasks: ProfileTaskTree;
  readonly preConditionTasks: ProfileTaskTree;
  readonly isRevocable?: boolean;

  static create<T extends Profile>(profileData: T): Profile {
    return plainToInstance(this, profileData);
  }
}
