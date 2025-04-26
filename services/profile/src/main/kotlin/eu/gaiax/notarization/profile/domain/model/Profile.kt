package eu.gaiax.notarization.profile.domain.model

import eu.gaiax.notarization.api.profile.*
import eu.gaiax.notarization.profile.infrastructure.config.ConflictingCredentialConfigurationException
import eu.gaiax.notarization.profile.infrastructure.config.InvalidActionTreeConfigurationException
import eu.gaiax.notarization.profile.infrastructure.config.InvalidTaskTreeConfigurationException
import eu.gaiax.notarization.profile.infrastructure.config.MissingCredentialKindException
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

@Throws(
    InvalidTaskTreeConfigurationException::class,
    ConflictingCredentialConfigurationException::class
)
fun Profile.assertTreesValid() {
    val kind = kind
    val aip = aip
    if (kind != null && aip != null) {
        if (aip.asCredentialKind() != kind) {
            throw ConflictingCredentialConfigurationException(aip, kind, "The given AIP profile $aip conflicts with the credential kind $kind")
        }
    }
    if (kind == null && aip == null) {
        throw MissingCredentialKindException("The mandatory field 'kind' is missing.")
    }

    if (!tasks.structureValid()) throw InvalidTaskTreeConfigurationException("Tasks tree structure of profile \"$name\" not valid.")
    if (!preconditionTasks.structureValid()) throw InvalidTaskTreeConfigurationException("Preconditiontasks tree structure of profile \"$name\" not valid.")
    if (preconditionTasks.fulfilledByDefault()) {
        logger.warn(
            """
                The precondition tree defines tasks, but the tree defines at least one path which fulfills the tree,
                without performing any task (all tasks are optional).
                This basically says that there is a precondition which has to be fulfilled before any other action is allowed,
                but this condition is imediately fulfilled. (You MUST perform an OPTIONAL task)
                If this is intended, consider putting the tasks in the "normal" task tree, which has the same effect without possible confusion.
                """
        )
    }
    if (!preIssuanceActions.structureValid()) throw InvalidActionTreeConfigurationException("Actions tree structure of profile \"$name\" not valid.")

    val taskDescriptionsByName = taskDescriptions.associateBy { it.name };
    val actionDescriptionsByName = actionDescriptions.associateBy { it.name };

    val tasksEvaluation = { name: String -> !taskDescriptionsByName.containsKey(name) }
    validateNames(tasks, tasksEvaluation)
        { taskName: String -> "The task \"$taskName\" of profile: \"$name\" in tasks tree is missing in taskDescriptions." }
    validateNames(preconditionTasks, tasksEvaluation)
        { taskName: String -> "The task \"$taskName\" of profile: \"$name\" in preconditiontasks tree is missing in taskDescriptions." }
    validateNames(preIssuanceActions, { actionName -> !actionDescriptionsByName.containsKey(actionName) })
        { taskName: String -> "The action \"$taskName\" of profile: \"$name\" in preIssuanceActions tree is missing in actionDescriptions." }
}

private fun Profile.validateNames(
    input: ProfileTaskTree,
    evaluation: (String) -> Boolean,
    erroMessage: (String) -> String
) {
    val missingTaskName: String? = input.allNames().firstOrNull(evaluation)
    if (missingTaskName != null) {
        throw InvalidTaskTreeConfigurationException(erroMessage(missingTaskName))
    }
}
