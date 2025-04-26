package eu.xfsc.not.oid4vp

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import mu.KotlinLogging

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class TrainMock

private val logger = KotlinLogging.logger {}
class TrainMockImp : QuarkusTestResourceLifecycleManager {

    private var wireMockServer: WireMockServer? = null

    override fun start(): MutableMap<String, String> {
        wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
        wireMockServer?.let { it.start() }

        return mutableMapOf(("quarkus.rest-client.train_api.url" to wireMockServer!!.baseUrl()))
    }

    companion object TEST {
        private val responseContainingEndpoints = """
       {
    "trustSchemePointers": [
        {
            "pointer": "alice.trust.train1.xfsc.dev",
            "dids": [
                "did:web:essif.iao.fraunhofer.de"
            ],
            "error": null
        }
    ],
    "resolvedResults": [
        {
            "did": "did:web:essif.iao.fraunhofer.de",
            "resolvedDoc": {
                "document": {
                    "@context": [
                        "https://www.w3.org/ns/did/v1",
                        "https://w3id.org/security/suites/jws-2020/v1"
                    ],
                    "id": "did:web:essif.iao.fraunhofer.de",
                    "verificationMethod": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#owner",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "kty": "OKP",
                                "crv": "Ed25519",
                                "x": "yaHbNw6nj4Pn3nGPHyyTqP-QHXYNJIpkA37PrIOND4c"
                            }
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#test",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "crv": "P-256",
                                "kid": "test",
                                "kty": "EC",
                                "x": "IglrRKSINwyxro6sT4WKy-mowDW2io3b3jL9LML8a-A",
                                "y": "IQ8l61-wV0mH4ND_O-hEcr-8SY1u8EivybLeMH3a_bM"
                            }
                        }
                    ],
                    "service": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer",
                            "type": "gx-trust-list-issuer",
                            "serviceEndpoint": "http://fed1-tfm:8080/tspa-service/tspa/v1/workshop-test.federation1.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-federation2",
                            "type": "gx-trust-list-issuer-federation2",
                            "serviceEndpoint": "http://fed2-tfm:8080/tspa-service/tspa/v1/workshop-test.federation2.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-xml",
                            "type": "gx-trust-list-issuer-public-xml",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/alice.trust.train1.xfsc.dev/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-json",
                            "type": "gx-trust-list-issuer-public-json",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/bob.trust.train1.xfsc.dev/vc/trust-list"
                        }
                    ],
                    "authentication": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ],
                    "assertionMethod": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ]
                },
                "endpoints": [{}],
                "didVerified": true
            },
            "error": {
                "code": "did_error",
                "message": "JsonLdError[code=The document could not be loaded or parsed [code=LOADING_DOCUMENT_FAILED]., message=Unexpected response code [404]]"
            }
        }
    ]
}
    """.trimIndent()
        private val responseNotContainingEndpoints = """
       {
    "trustSchemePointers": [
        {
            "pointer": "alice.trust.train1.xfsc.dev",
            "dids": [
                "did:web:essif.iao.fraunhofer.de"
            ],
            "error": null
        }
    ],
    "resolvedResults": [
        {
            "did": "did:web:essif.iao.fraunhofer.de",
            "resolvedDoc": {
                "document": {
                    "@context": [
                        "https://www.w3.org/ns/did/v1",
                        "https://w3id.org/security/suites/jws-2020/v1"
                    ],
                    "id": "did:web:essif.iao.fraunhofer.de",
                    "verificationMethod": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#owner",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "kty": "OKP",
                                "crv": "Ed25519",
                                "x": "yaHbNw6nj4Pn3nGPHyyTqP-QHXYNJIpkA37PrIOND4c"
                            }
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#test",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "crv": "P-256",
                                "kid": "test",
                                "kty": "EC",
                                "x": "IglrRKSINwyxro6sT4WKy-mowDW2io3b3jL9LML8a-A",
                                "y": "IQ8l61-wV0mH4ND_O-hEcr-8SY1u8EivybLeMH3a_bM"
                            }
                        }
                    ],
                    "service": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer",
                            "type": "gx-trust-list-issuer",
                            "serviceEndpoint": "http://fed1-tfm:8080/tspa-service/tspa/v1/workshop-test.federation1.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-federation2",
                            "type": "gx-trust-list-issuer-federation2",
                            "serviceEndpoint": "http://fed2-tfm:8080/tspa-service/tspa/v1/workshop-test.federation2.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-xml",
                            "type": "gx-trust-list-issuer-public-xml",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/alice.trust.train1.xfsc.dev/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-json",
                            "type": "gx-trust-list-issuer-public-json",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/bob.trust.train1.xfsc.dev/vc/trust-list"
                        }
                    ],
                    "authentication": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ],
                    "assertionMethod": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ]
                },
                "endpoints": null,
                "didVerified": true
            },
            "error": {
                "code": "did_error",
                "message": "JsonLdError[code=The document could not be loaded or parsed [code=LOADING_DOCUMENT_FAILED]., message=Unexpected response code [404]]"
            }
        }
    ]
}
    """.trimIndent()
        private val responseNoResults= """
       {
    "trustSchemePointers": [
        {
            "pointer": "alice.trust.train1.xfsc.dev",
            "dids": [],
            "error": null
        }
    ],
    "resolvedResults": []
}
    """.trimIndent()
        private val responseOneDidVerifiedFalse= """
       {
    "trustSchemePointers": [
        {
            "pointer": "alice.trust.train1.xfsc.dev",
            "dids": [
                "did:web:essif.iao.fraunhofer.de"
            ],
            "error": null
        }
    ],
    "resolvedResults": [
    {
            "did": "did:web:essif.iao.fraunhofer.de",
            "resolvedDoc": {
                "document": {
                    "@context": [
                        "https://www.w3.org/ns/did/v1",
                        "https://w3id.org/security/suites/jws-2020/v1"
                    ],
                    "id": "did:web:essif.iao.fraunhofer.de",
                    "verificationMethod": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#owner",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "kty": "OKP",
                                "crv": "Ed25519",
                                "x": "yaHbNw6nj4Pn3nGPHyyTqP-QHXYNJIpkA37PrIOND4c"
                            }
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#test",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "crv": "P-256",
                                "kid": "test",
                                "kty": "EC",
                                "x": "IglrRKSINwyxro6sT4WKy-mowDW2io3b3jL9LML8a-A",
                                "y": "IQ8l61-wV0mH4ND_O-hEcr-8SY1u8EivybLeMH3a_bM"
                            }
                        }
                    ],
                    "service": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer",
                            "type": "gx-trust-list-issuer",
                            "serviceEndpoint": "http://fed1-tfm:8080/tspa-service/tspa/v1/workshop-test.federation1.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-federation2",
                            "type": "gx-trust-list-issuer-federation2",
                            "serviceEndpoint": "http://fed2-tfm:8080/tspa-service/tspa/v1/workshop-test.federation2.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-xml",
                            "type": "gx-trust-list-issuer-public-xml",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/alice.trust.train1.xfsc.dev/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-json",
                            "type": "gx-trust-list-issuer-public-json",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/bob.trust.train1.xfsc.dev/vc/trust-list"
                        }
                    ],
                    "authentication": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ],
                    "assertionMethod": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ]
                },
                "endpoints": [{}],
                "didVerified": true
            },
            "error": {
                "code": "did_error",
                "message": "JsonLdError[code=The document could not be loaded or parsed [code=LOADING_DOCUMENT_FAILED]., message=Unexpected response code [404]]"
            }
        },
        {
            "did": "did:web:essif.iao.fraunhofer.de",
            "resolvedDoc": {
                "document": {
                    "@context": [
                        "https://www.w3.org/ns/did/v1",
                        "https://w3id.org/security/suites/jws-2020/v1"
                    ],
                    "id": "did:web:essif.iao.fraunhofer.de",
                    "verificationMethod": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#owner",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "kty": "OKP",
                                "crv": "Ed25519",
                                "x": "yaHbNw6nj4Pn3nGPHyyTqP-QHXYNJIpkA37PrIOND4c"
                            }
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#test",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "crv": "P-256",
                                "kid": "test",
                                "kty": "EC",
                                "x": "IglrRKSINwyxro6sT4WKy-mowDW2io3b3jL9LML8a-A",
                                "y": "IQ8l61-wV0mH4ND_O-hEcr-8SY1u8EivybLeMH3a_bM"
                            }
                        }
                    ],
                    "service": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer",
                            "type": "gx-trust-list-issuer",
                            "serviceEndpoint": "http://fed1-tfm:8080/tspa-service/tspa/v1/workshop-test.federation1.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-federation2",
                            "type": "gx-trust-list-issuer-federation2",
                            "serviceEndpoint": "http://fed2-tfm:8080/tspa-service/tspa/v1/workshop-test.federation2.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-xml",
                            "type": "gx-trust-list-issuer-public-xml",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/alice.trust.train1.xfsc.dev/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-json",
                            "type": "gx-trust-list-issuer-public-json",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/bob.trust.train1.xfsc.dev/vc/trust-list"
                        }
                    ],
                    "authentication": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ],
                    "assertionMethod": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ]
                },
                "endpoints": [{}],
                "didVerified": false
            },
            "error": {
                "code": "did_error",
                "message": "JsonLdError[code=The document could not be loaded or parsed [code=LOADING_DOCUMENT_FAILED]., message=Unexpected response code [404]]"
            }
        }
    ]
}
    """.trimIndent()

        private val responseDidVerifiedFalse= """
       {
    "trustSchemePointers": [
        {
            "pointer": "alice.trust.train1.xfsc.dev",
            "dids": [
                "did:web:essif.iao.fraunhofer.de"
            ],
            "error": null
        }
    ],
    "resolvedResults": [
        {
            "did": "did:web:essif.iao.fraunhofer.de",
            "resolvedDoc": {
                "document": {
                    "@context": [
                        "https://www.w3.org/ns/did/v1",
                        "https://w3id.org/security/suites/jws-2020/v1"
                    ],
                    "id": "did:web:essif.iao.fraunhofer.de",
                    "verificationMethod": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#owner",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "kty": "OKP",
                                "crv": "Ed25519",
                                "x": "yaHbNw6nj4Pn3nGPHyyTqP-QHXYNJIpkA37PrIOND4c"
                            }
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#test",
                            "type": "JsonWebKey2020",
                            "controller": "did:web:essif.iao.fraunhofer.de",
                            "publicKeyJwk": {
                                "crv": "P-256",
                                "kid": "test",
                                "kty": "EC",
                                "x": "IglrRKSINwyxro6sT4WKy-mowDW2io3b3jL9LML8a-A",
                                "y": "IQ8l61-wV0mH4ND_O-hEcr-8SY1u8EivybLeMH3a_bM"
                            }
                        }
                    ],
                    "service": [
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer",
                            "type": "gx-trust-list-issuer",
                            "serviceEndpoint": "http://fed1-tfm:8080/tspa-service/tspa/v1/workshop-test.federation1.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-federation2",
                            "type": "gx-trust-list-issuer-federation2",
                            "serviceEndpoint": "http://fed2-tfm:8080/tspa-service/tspa/v1/workshop-test.federation2.train/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-xml",
                            "type": "gx-trust-list-issuer-public-xml",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/alice.trust.train1.xfsc.dev/vc/trust-list"
                        },
                        {
                            "id": "did:web:essif.iao.fraunhofer.de#gx-trust-list-issuer-public-json",
                            "type": "gx-trust-list-issuer-public-json",
                            "serviceEndpoint": "https://tspa.train1.xfsc.dev/tspa-service/tspa/v1/bob.trust.train1.xfsc.dev/vc/trust-list"
                        }
                    ],
                    "authentication": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ],
                    "assertionMethod": [
                        "did:web:essif.iao.fraunhofer.de#owner"
                    ]
                },
                "endpoints": [{}],
                "didVerified": true
            },
            "error": {
                "code": "did_error",
                "message": "JsonLdError[code=The document could not be loaded or parsed [code=LOADING_DOCUMENT_FAILED]., message=Unexpected response code [404]]"
            }
        }
    ]
}
    """.trimIndent()


        private fun setStub(wireMockServer: WireMockServer, response: String){
            wireMockServer.resetAll()
            wireMockServer.apply {
                stubFor(
                    WireMock.post(
                        WireMock.urlMatching("/resolve")
                    )
                        .willReturn(
                            WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(response)
                        )
                )
            }
        }

        fun setStubValidAnswer(wireMockServer: WireMockServer){
            setStub(wireMockServer, responseContainingEndpoints)
        }
        fun setStubNoResults(wireMockServer: WireMockServer){
            setStub(wireMockServer, responseNoResults)
        }
        fun setStubDidUnverified(wireMockServer: WireMockServer){
            setStub(wireMockServer, responseDidVerifiedFalse)
        }
        fun setStubNoEndpoints(wireMockServer: WireMockServer){
            setStub(wireMockServer, responseNotContainingEndpoints)
        }
        fun setStubOneDidUnverified(wireMockServer: WireMockServer) {
            setStub(wireMockServer, responseOneDidVerifiedFalse)
        }
    }

    @Synchronized
    override fun stop() {
        wireMockServer?.let {
            it.stop()
            wireMockServer = null
        }
    }

    override fun inject(testInjector: QuarkusTestResourceLifecycleManager.TestInjector) {
        testInjector.injectIntoFields(
            wireMockServer,
            QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType(
                TrainMock::class.java, WireMockServer::class.java
            )
        )
    }
}
