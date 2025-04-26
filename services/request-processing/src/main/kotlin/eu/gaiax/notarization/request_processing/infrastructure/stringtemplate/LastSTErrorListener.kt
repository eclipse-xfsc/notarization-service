/*
 *
 */
package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import org.stringtemplate.v4.STErrorListener
import org.stringtemplate.v4.misc.STMessage

/**
 *
 * @author Neil Crossley
 */
class LastSTErrorListener : STErrorListener {
    private var last: STMessage? = null
    override fun compileTimeError(msg: STMessage) {
        last = msg
    }

    override fun runTimeError(msg: STMessage) {
        last = msg
    }

    override fun IOError(msg: STMessage) {
        last = msg
    }

    override fun internalError(msg: STMessage) {
        last = msg
    }

    fun lastError(): STMessage? {
        return last
    }
}
