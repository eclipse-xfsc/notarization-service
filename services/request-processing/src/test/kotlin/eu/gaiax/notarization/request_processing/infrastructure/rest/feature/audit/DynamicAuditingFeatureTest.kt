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
package eu.gaiax.notarization.request_processing.infrastructure.rest.feature.audit

import eu.gaiax.notarization.MockServicesLifecycleManager
import eu.gaiax.notarization.request_processing.matcher.IsAnnotationPresent
import eu.gaiax.notarization.request_processing.matcher.IsAssignableTo
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.ws.rs.container.DynamicFeature
import jakarta.ws.rs.ext.Provider
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Test
import org.junitpioneer.jupiter.ReportEntry

/**
 *
 * @author Neil Crossley
 */
@QuarkusTest
@QuarkusTestResource(MockServicesLifecycleManager::class)
class DynamicAuditingFeatureTest {
    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00015")
    fun sutIsDynamicFeature() {
        MatcherAssert.assertThat<Class<DynamicAuditingFeature>>(
            DynamicAuditingFeature::class.java, IsAssignableTo.Companion.isAssignableTo<DynamicFeature>(
                DynamicFeature::class.java
            )
        )
    }

    @Test
    @ReportEntry(key = "REQUIREMENT", value = "CMP.NA.00015")
    fun sutIsProvider() {
        MatcherAssert.assertThat<Class<DynamicAuditingFeature>>(
            DynamicAuditingFeature::class.java, IsAnnotationPresent.Companion.isAnnotationPresent<Provider>(
                Provider::class.java
            )
        )
    }
}
