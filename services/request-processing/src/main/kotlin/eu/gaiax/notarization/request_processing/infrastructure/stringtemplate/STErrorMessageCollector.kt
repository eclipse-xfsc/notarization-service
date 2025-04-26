package eu.gaiax.notarization.request_processing.infrastructure.stringtemplate

import org.stringtemplate.v4.STErrorListener
import org.stringtemplate.v4.misc.STMessage

class STErrorMessageCollector: STErrorListener {
    val errors: MutableList<Pair<ErrorType, STMessage>> = mutableListOf()
    private var last: STMessage? = null

    fun lastError(): STMessage? {
        return last
    }

    fun hasAnyErrors(): Boolean {
        return errors.isNotEmpty()
    }

    override fun compileTimeError(msg: STMessage) {
        addError(ErrorType.compileTime, msg)
    }

    override fun runTimeError(msg: STMessage) {
        addError(ErrorType.runTime, msg)
    }

    override fun IOError(msg: STMessage) {
        addError(ErrorType.io, msg)
    }

    override fun internalError(msg: STMessage) {
        addError(ErrorType.internal, msg)
    }

    private fun addError(type: ErrorType, msg: STMessage) {
        last = msg
        errors.add(type to msg)
    }

    override fun toString(): String {
        if (this.errors.isEmpty()) {
            return "No ST messages collected"
        }
        val builder = StringBuilder()
        builder.append("Collected a total of ")
        builder.append(errors.size)
        builder.append(" ST error messages. They are:")
        for (entry in this.errors) {
            builder.appendLine()
            builder.append(entry.first)
            builder.append(" error: ")
            builder.append(entry.second)
        }
        return builder.toString()
    }

    companion object {
        enum class ErrorType {
            compileTime,
            runTime,
            io,
            internal
        }

    }
}
