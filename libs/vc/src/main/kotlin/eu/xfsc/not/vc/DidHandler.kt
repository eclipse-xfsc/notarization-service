package eu.xfsc.not.vc

import id.walt.crypto.keys.Key
import id.walt.did.dids.DidService
import id.walt.did.dids.document.DidDocument
import id.walt.did.dids.registrar.DidRegistrar
import id.walt.did.dids.registrar.DidResult
import id.walt.did.dids.registrar.LocalRegistrar
import id.walt.did.dids.registrar.UniregistrarRegistrar
import id.walt.did.dids.registrar.dids.DidCreateOptions
import id.walt.did.dids.resolver.DidResolver
import id.walt.did.dids.resolver.LocalResolver
import id.walt.did.dids.resolver.UniresolverResolver
import kotlinx.coroutines.runBlocking


object DidHandler {
    fun init(
        uniResolverUrl: String? = null,
        uniRegistrarUrl: String? = null,
        withUniResolver: Boolean = true,
        preferLocalResolver: Boolean = true,
        withUniRegistrar: Boolean = true,
        preferLocalRegistrar: Boolean = true
    ) = runBlocking {
        DidService.didResolvers.clear()
        DidService.didRegistrars.clear()
        DidService.resolverMethods.clear()
        DidService.registrarMethods.clear()

        // assemble and register resolvers
        val uniResolver = UniresolverResolver()
        if (uniResolverUrl != null) {
            uniResolver.resolverUrl = uniResolverUrl
        }

        var resolvers = mutableSetOf<DidResolver>()
        resolvers.add(LocalResolver())
        if (withUniResolver) {
            resolvers.add(uniResolver)
        }
        if (!preferLocalResolver) {
            resolvers = resolvers.reversed().toMutableSet()
        }
        DidService.registerAllResolvers(resolvers)

        // assemble and register registrars
        val uniRegistrar = UniregistrarRegistrar()
        if (uniRegistrarUrl != null) {
            uniRegistrar.registrarUrl = uniRegistrarUrl
        }

        var registrars = mutableSetOf<DidRegistrar>()
        registrars.add(LocalRegistrar())
        if (withUniRegistrar) {
            registrars.add(uniRegistrar)
        }
        if (!preferLocalRegistrar) {
            registrars = registrars.reversed().toMutableSet()
        }
        DidService.registerAllRegistrars(registrars)

        DidService.updateResolversForMethods()
        DidService.updateRegistrarsForMethods()
    }

    fun resolve(did: String): Result<DidDocument> = runBlocking {
        DidService.resolve(did).mapCatching {
            DidDocument(it)
        }
    }

     fun resolveKey(did: String): Result<Key> = runBlocking {
         DidService.resolveToKey(did)
     }

    fun registerDidKey(key: Key, options: DidCreateOptions): DidResult = runBlocking {
        DidService.registerByKey(options.method, key, options)
    }
}
