package eu.gaiax.notarization.environment

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

@ApplicationScoped
class DidWebGenerator {

    private val didWeb: String = "did:web:eid.services:not-api"

    fun generateWebDid() : CompletableFuture<String> = GlobalScope.future {
        doWebDidGeneration()
    }

    private suspend fun doWebDidGeneration() : String {
        // This code can be used to generate a did-web
        // The did document must be hosted at the defined host
        // In this example at https://eid.services/not-api/did.json
        /*DidService.minimalInit()

        val localKey = LocalKey.generate(type = KeyType.Ed25519)
        val didOpts = DidWebCreateOptions("eid.services", "not-api", KeyType.Ed25519)
        val didResult = DidService.registerByKey(method = didOpts.method, key = localKey, options = didOpts)

        io.quarkus.logging.Log.info(didResult.didDocument.toJsonObject().toString())

        return didResult.did*/
        return didWeb
    }
}
