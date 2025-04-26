/*
 *
 */
package eu.gaiax.notarization.request_processing.domain.model

/**
 *
 * @author Neil Crossley
 */
class TemplateModel private constructor() {
    interface Document {
        val id: String?
        val title: String?
        val shortDescription: String?
        val mimetype: String?
        val extension: String?
        val sha3_256: DocumentHash?
        val sha3: DocumentHash?
        val sha: DocumentHash?
        val self: Document?
    }

    interface DocumentHash {
        val hex: String?
        val base: String?
        val base64: String?
        val base64URLSafe: String?
    }
}
