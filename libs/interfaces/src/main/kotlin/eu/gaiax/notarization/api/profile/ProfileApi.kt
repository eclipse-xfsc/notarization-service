/****************************************************************************
 * Copyright 2022 ecsec GmbH
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
 */
package eu.gaiax.notarization.api.profile

/**
 *
 * @author Neil Crossley
 */
class ProfileApi private constructor() {
    object Path {
        const val PREFIX = "api"
        const val V1_PREFIX = "$PREFIX/v1"
        const val PROFILES = "profiles"
        const val PROFILE_IDENTIFIERS = "profile-ids"
        const val ROUTINES = "routines"
        const val PROTECTED = "protected"
        const val PROFILE_RESOURCE = "$V1_PREFIX/$PROFILES"
        const val PROFILE_IDENTIFIERS_RESOURCE = "$V1_PREFIX/$PROFILE_IDENTIFIERS"
        const val PROTECTED_PROFILE_RESOURCE = "$V1_PREFIX/$PROTECTED/$PROFILES"
        const val SSI_DATA = PROFILES + "/" + Param.PROFILE_ID_PARAM + "/ssi-data"
        const val SSI_DATA_V1 = PROFILES + "/" + Param.PROFILE_ID_PARAM + "/ssi-data/v1"
        const val SSI_DATA_V2 = PROFILES + "/" + Param.PROFILE_ID_PARAM + "/ssi-data/v2"
        const val ROUTINE_RESOURCE = "$V1_PREFIX/$ROUTINES"
    }

    object Param {
        const val PROFILE_ID = "profileId"
        const val PROFILE_ID_PARAM = "{$PROFILE_ID}"
    }
}
