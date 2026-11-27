package com.bhaskar.jarvis.core

/** Outcome of one executed step. Success must carry evidence that was observed. */
sealed interface StepResult {
    data class Success(val evidence: String) : StepResult

    data class Failure(
        val reason: FailureReason,
        val message: String,
        val candidates: List<String> = emptyList(),
    ) : StepResult
}

enum class FailureReason {
    NOT_FOUND,
    AMBIGUOUS,
    TIMEOUT,
    NOT_CLICKABLE,
    NOT_EDITABLE,
    PASSWORD_FIELD,
    SERVICE_DISABLED,
    UNKNOWN,
}
