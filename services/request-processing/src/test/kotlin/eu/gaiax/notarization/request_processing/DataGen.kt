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
package eu.gaiax.notarization.request_processing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import eu.gaiax.notarization.request_processing.domain.model.DocumentId
import eu.gaiax.notarization.request_processing.infrastructure.rest.dto.DocumentUploadByLink
import java.net.URI
import java.net.URISyntaxException
import java.util.*

/**
 *
 * @author Florian Otto
 */
object DataGen {
    private const val MAX_DEPTH = 5
    private const val MAX_FIELDNUMBER = 25
    private const val MAX_STR_LEN = 512
    private const val MAX_BYTEA_LEN = 512
    private const val MAX_LST_LEN = 50
    private val random = Random()
    fun genInt(bound: Int): Int {
        return random.nextInt(bound)
    }

    fun genInt(origin: Int, bound: Int): Int {
        return random.nextInt(origin, bound)
    }

    @JvmOverloads
    fun genBytes(len: Int = MAX_BYTEA_LEN): ByteArray {
        val randlen = random.nextInt(len)
        val buf = ByteArray(randlen)
        random.nextBytes(buf)
        if (randlen > 0) {
            buf[0] = 0
        }
        return buf
    }

    @JvmStatic
    @JvmOverloads
    fun genString(len: Int = MAX_STR_LEN): String {
        val randlen = random.nextInt(len)
        val buf = ByteArray(randlen)
        random.nextBytes(buf)
        if (randlen > 0) {
            buf[0] = 0
        }
        return String(buf)
    }

    @JvmOverloads
    fun genList(len: Int = MAX_LST_LEN): List<Int> {
        val lst = ArrayList<Int>()
        val nbr = random.nextInt(len)
        for (i in 0 until nbr) {
            lst.add(random.nextInt())
        }
        return lst
    }

    fun createRandomUploadDocumentData(): DocumentUploadByLink {
        return try {
            val d = DocumentUploadByLink()
            d.id = DocumentId(UUID.randomUUID())
            d.location = URI("http://localhost/~florian/link")
            d.title = genString(25)
            d.shortDescription = genString(128)
            d.longDescription = genString(512)
            d
        } catch (ex: URISyntaxException) {
            throw RuntimeException()
        }
    }

    val generators = arrayOf(
        { genString() },
        { genBytes() },
        { random.nextInt() },
        { random.nextBoolean() },
        { random.nextDouble() },
        { genList() }
    )

    fun createRandomJsonData(): JsonNode {
        return createRandomJsonData((Math.random() * MAX_DEPTH).toInt())
    }

    fun createRandomJsonLdData(): JsonNode {
        val json = createRandomJsonData((Math.random() * MAX_DEPTH).toInt())
        json.put("@context", "http://schema.org/")
        json.put("@type", "Person")
        return json
    }

    private fun createRandomJsonData(depth: Int): ObjectNode {
        var depth = depth
        val data = ObjectMapper().createObjectNode()
        val nbr = random.nextInt(MAX_FIELDNUMBER)
        for (i in 0 until nbr) {
            val generator = generators[random.nextInt(generators.size)]
            if (depth > 0) {
                data.putPOJO("key_" + i + "_" + depth, createRandomJsonData(--depth))
            } else {
                data.putPOJO("key_$i", generator())
            }
        }
        return data
    }

    interface Generator<T> {
        fun gen(): T
    }
}
