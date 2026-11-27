package com.bhaskar.jarvis.device

import android.content.Context
import android.content.Intent
import com.bhaskar.jarvis.core.Command
import com.bhaskar.jarvis.core.FailureReason
import com.bhaskar.jarvis.core.StepResult
import com.bhaskar.jarvis.core.apps.AppMatch
import com.bhaskar.jarvis.core.apps.AppMatcher
import com.bhaskar.jarvis.core.apps.InstalledApp
import com.bhaskar.jarvis.core.screen.ElementFinder
import com.bhaskar.jarvis.core.screen.InputFinder
import com.bhaskar.jarvis.core.screen.InputResolution
import com.bhaskar.jarvis.core.screen.ScreenFingerprint
import com.bhaskar.jarvis.core.screen.ScreenNode
import com.bhaskar.jarvis.core.screen.TapResolution
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs one validated [Command] and reports a verified [StepResult].
 *
 * Every step is checked against what the screen shows afterwards. A tap or
 * text entry that cannot be observed is reported as a failure, not a success.
 * Call from the main thread.
 */
class DeviceController(private val context: Context) {

    suspend fun run(command: Command): StepResult = when (command) {
        is Command.OpenApp -> openApp(command.appName)
        is Command.TapText -> tapText(command.visibleText)
        is Command.TypeText -> typeText(command.text)
    }

    suspend fun openApp(name: String): StepResult {
        val service = JarvisAccessibilityService.instance ?: return serviceDisabled()
        val packageManager = context.packageManager

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcherIntent, 0)
            .map {
                InstalledApp(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                )
            }
            .distinct()

        return when (val match = AppMatcher.match(name, apps)) {
            is AppMatch.NotFound -> StepResult.Failure(
                FailureReason.NOT_FOUND,
                "No installed app is named \"$name\".",
                match.suggestions,
            )
            is AppMatch.Ambiguous -> StepResult.Failure(
                FailureReason.AMBIGUOUS,
                "More than one app is named \"$name\". Use a more specific name.",
                match.candidates.map { "${it.label} (${it.packageName})" },
            )
            is AppMatch.Found -> launch(service, match.app)
        }
    }

    private suspend fun launch(service: JarvisAccessibilityService, app: InstalledApp): StepResult {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            ?: return StepResult.Failure(FailureReason.NOT_FOUND, "${app.label} cannot be launched.")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        val reached = withTimeoutOrNull(OPEN_TIMEOUT_MS) {
            while (service.foregroundPackage() != app.packageName) delay(POLL_MS)
            true
        } == true

        return if (reached) {
            StepResult.Success("${app.label} is in the foreground.")
        } else {
            StepResult.Failure(FailureReason.TIMEOUT, "${app.label} did not come to the foreground in time.")
        }
    }

    suspend fun tapText(label: String): StepResult {
        val service = JarvisAccessibilityService.instance ?: return serviceDisabled()
        val before = service.snapshot()
            ?: return StepResult.Failure(FailureReason.UNKNOWN, "No readable window is active.")

        return when (val resolution = ElementFinder.resolveTap(before, label)) {
            is TapResolution.NotFound -> StepResult.Failure(
                FailureReason.NOT_FOUND,
                "No visible control is labelled \"$label\".",
            )
            is TapResolution.NotClickable -> StepResult.Failure(
                FailureReason.NOT_CLICKABLE,
                "\"${resolution.label}\" is on screen but cannot be tapped.",
            )
            is TapResolution.Ambiguous -> StepResult.Failure(
                FailureReason.AMBIGUOUS,
                "More than one control is labelled \"$label\".",
                resolution.labels,
            )
            is TapResolution.Found -> {
                if (!service.clickAt(resolution.target.id)) {
                    return StepResult.Failure(FailureReason.UNKNOWN, "The system rejected the tap.")
                }
                if (awaitScreenChange(service, before)) {
                    StepResult.Success("Tapped \"$label\" and the screen changed.")
                } else {
                    StepResult.Failure(
                        FailureReason.UNKNOWN,
                        "Tapped \"$label\", but no change was seen on screen.",
                    )
                }
            }
        }
    }

    suspend fun typeText(text: String): StepResult {
        val service = JarvisAccessibilityService.instance ?: return serviceDisabled()
        val before = service.snapshot()
            ?: return StepResult.Failure(FailureReason.UNKNOWN, "No readable window is active.")

        return when (val resolution = InputFinder.resolveFocusedEditable(before)) {
            is InputResolution.NoFocusedField -> StepResult.Failure(
                FailureReason.NOT_EDITABLE,
                "Tap a text field first, then run the command again.",
            )
            is InputResolution.NotEditable -> StepResult.Failure(
                FailureReason.NOT_EDITABLE,
                "The focused item is not a text field.",
            )
            is InputResolution.PasswordField -> StepResult.Failure(
                FailureReason.PASSWORD_FIELD,
                "Jarvis does not type into password fields.",
            )
            is InputResolution.Ambiguous -> StepResult.Failure(
                FailureReason.AMBIGUOUS,
                "${resolution.count} text fields have focus. Leave only one focused.",
            )
            is InputResolution.Found -> {
                val fieldId = resolution.target.id
                if (!service.setTextAt(fieldId, text)) {
                    return StepResult.Failure(FailureReason.UNKNOWN, "The system rejected the text input.")
                }
                // Read the field back. The text itself is never included in the result.
                val after = service.snapshot()?.let { findById(it, fieldId) }
                if (after?.text == text) {
                    StepResult.Success("Text entered and verified in the field.")
                } else {
                    StepResult.Failure(
                        FailureReason.UNKNOWN,
                        "Text was sent, but the field does not show it.",
                    )
                }
            }
        }
    }

    private suspend fun awaitScreenChange(service: JarvisAccessibilityService, before: ScreenNode): Boolean {
        val beforeFingerprint = ScreenFingerprint.of(before)
        return withTimeoutOrNull(TAP_TIMEOUT_MS) {
            var changed = false
            while (!changed) {
                delay(POLL_MS)
                changed = ScreenFingerprint.of(service.snapshot()) != beforeFingerprint
            }
            true
        } == true
    }

    private fun findById(node: ScreenNode, id: String): ScreenNode? {
        if (node.id == id) return node
        for (child in node.children) {
            findById(child, id)?.let { return it }
        }
        return null
    }

    private fun serviceDisabled() = StepResult.Failure(
        FailureReason.SERVICE_DISABLED,
        "Turn on the Jarvis accessibility service in Settings, then try again.",
    )

    private companion object {
        const val POLL_MS = 250L
        const val OPEN_TIMEOUT_MS = 8_000L
        const val TAP_TIMEOUT_MS = 3_000L
    }
}
