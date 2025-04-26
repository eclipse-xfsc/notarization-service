package eu.xfsc.not.ssi_issuance2.ldp

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object LdpVcExamples {

    private val mapper = jacksonObjectMapper()

    fun asObjectNode(value: String): ObjectNode {
        return mapper.readValue(value, ObjectNode::class.java)
    }

    object V1 {
        val example1: ObjectNode
            inline get() = asObjectNode(
                """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
              ],
              "type": ["VerifiableCredential", "AlumniCredential"],
              "issuer": "https://example.edu/issuers/565049",
              "credentialSubject": {
                "alumniOf": {
                  "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                  "name": [{
                    "value": "Example University",
                    "lang": "en"
                  }, {
                    "value": "Exemple d'Université",
                    "lang": "fr"
                  }]
                }
              }
            }
        """.trimIndent()
            )
    }

    object V2 {
        val example1: ObjectNode
            inline get() = asObjectNode(
                """
                {
                  "@context": [
                    "https://www.w3.org/ns/credentials/v2",
                    "https://www.w3.org/ns/credentials/examples/v2"
                  ],
                  "id": "urn:uuid:6a9c92a9-2530-4e2b-9776-530467e9bbe0",
                  "type": ["VerifiableCredential", "ExampleAlumniCredential"],
                  "issuer": "https://university.example/issuers/565049",
                  "validFrom": "2010-01-01T19:23:24Z",
                  "credentialSubject": {
                    "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                    "alumniOf": {
                      "id": "did:example:c276e12ec21ebfeb1f712ebc6f1",
                      "name": "Example University"
                    }
                  }
                }
                """.trimIndent()
            )
    }
}
