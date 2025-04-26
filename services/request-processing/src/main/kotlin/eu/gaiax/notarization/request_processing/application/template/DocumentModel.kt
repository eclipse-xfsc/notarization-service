/*
 *
 */
package eu.gaiax.notarization.request_processing.application.template

import eu.gaiax.notarization.request_processing.domain.entity.*
import eu.gaiax.notarization.request_processing.domain.model.*

/**
 *
 * @author Neil Crossley
 */
class DocumentModel(private val document: Document) : TemplateModel.Document {
    override val id: String
        get() = document.id.toString()
    override val title: String?
        get() = document.title
    override val shortDescription: String?
        get() = document.shortDescription
    override val mimetype: String?
        get() = document.mimetype
    override val extension: String?
        get() = document.extension
    override val sha3_256: TemplateModel.DocumentHash
        get() = DocumentHashModel.Companion.fromHex(document.hash)
    override val sha3: TemplateModel.DocumentHash
        get() = DocumentHashModel.Companion.fromHex(document.hash)
    override val sha: TemplateModel.DocumentHash
        get() = DocumentHashModel.Companion.fromHex(document.hash)
    override val self: TemplateModel.Document
        get() = this

    private class DocumentModelComparator : Comparator<DocumentModel> {
        override fun compare(left: DocumentModel, right: DocumentModel): Int {
            return left.id.compareTo(right.id)
        }

        companion object {
            val INSTANCE = DocumentModelComparator()
        }
    }

    companion object {
        fun fromDocument(documents: Set<Document>): List<DocumentModel> {
            val result = ArrayList<DocumentModel>(documents.size)
            for (document in documents) {
                result.add(DocumentModel(document))
            }
            result.sortWith(DocumentModelComparator.INSTANCE)
            return result
        }
    }
}
