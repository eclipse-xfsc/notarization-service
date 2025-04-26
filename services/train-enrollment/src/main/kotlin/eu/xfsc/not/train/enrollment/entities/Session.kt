/****************************************************************************
 * Copyright 2024 ecsec GmbH
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

package eu.xfsc.not.train.enrollment.entities

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.net.URL


/**
 *
 * @author Mike Prechtl
 */
@Entity(name = "session")
@Table(name = "session")
class Session : PanacheEntityBase {

    @Id
    lateinit var id: String
    lateinit var nonce: String
    lateinit var successURL: URL
    lateinit var failureURL: URL

    companion object: PanacheCompanionBase<Session, String> {
        fun findByNonceOptional(nonce: String) : Session? {
            return Session.find("nonce", nonce).firstResult()
        }
    }

}
