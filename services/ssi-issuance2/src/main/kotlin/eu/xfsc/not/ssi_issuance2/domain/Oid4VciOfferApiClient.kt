package eu.xfsc.not.ssi_issuance2.domain

import eu.xfsc.not.api.oid4vci.Oid4VciOfferApi
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "offer_api")
interface Oid4VciOfferApiClient : Oid4VciOfferApi
