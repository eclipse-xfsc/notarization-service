/*
 *
 */
package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import eu.gaiax.notarization.api.profile.Profile
import eu.gaiax.notarization.request_processing.application.template.DocumentModel
import eu.gaiax.notarization.request_processing.domain.entity.Document
import eu.gaiax.notarization.request_processing.domain.entity.NotarizationRequest
import jakarta.enterprise.context.ApplicationScoped
import mu.KotlinLogging
import org.stringtemplate.v4.*
import org.stringtemplate.v4.compiler.STException
import org.stringtemplate.v4.debug.ConstructionEvent

private val logger = KotlinLogging.logger {}


/**
 *
 * @author Neil Crossley
 */
@ApplicationScoped
class ProfileTemplateRenderer {
    fun render(profile: Profile, documentTemplate: String?, docs: Set<Document>, value: NotarizationRequest): String {
        val debugState = ST.DebugState()
        debugState.newSTEvent = ConstructionEvent()
        STGroup.trackCreationEvents = true
        val group: STGroup = group
        val errorListener = STErrorMessageCollector()
        group.listener = errorListener
        val template: ST
        try {
            group.defineTemplate(profile.id, "documents,credentialAugmentation,notaryAugment,augmentation", documentTemplate)
            template = group.getInstanceOf(profile.id)
            template.add("documents", DocumentModel.fromDocument(docs))
            template.debugState = debugState
        } catch (ex: STException) {
            throw IllegalStateException(
                "The template contains errors: " + errorListener.lastError().toString(), ex
            )
        }
        val augmentWrapper = JsonObjectSTWrapper.from(value.credentialAugmentation)
        template.add("credentialAugmentation", augmentWrapper)
        template.add("notaryAugment", augmentWrapper)
        template.add("augmentation", augmentWrapper)
        val rendered = template.render()
        if (errorListener.hasAnyErrors()) {
            logger.warn { "The template was rendered with errors:\n$errorListener" }
        }
        return rendered
    }

    val group: STGroup
        get() {

            val stGroup = STGroup()
            stGroup.registerModelAdaptor<JsonObjectSTWrapper>(
                JsonObjectSTWrapper::class.java,
                JsonObjectSTWrapperAdapter()
            )
            stGroup.registerModelAdaptor<PrefixedJsonObjectSTWrapper>(
                PrefixedJsonObjectSTWrapper::class.java,
                PrefixedJsonObjectSTWrapperAdapter()
            )
            stGroup.registerRenderer<JsonObjectSTWrapper>(
                JsonObjectSTWrapper::class.java,
                JsonObjectSTWrapperRenderer()
            )
            stGroup.registerRenderer<PrefixedJsonObjectSTWrapper>(
                PrefixedJsonObjectSTWrapper::class.java,
                PrefixedJsonObjectSTWrapperRenderer()
            )
            stGroup.registerRenderer<JsonArraySTWrapper>(JsonArraySTWrapper::class.java, JsonArraySTWrapperRenderer())

            return stGroup
        }
}
