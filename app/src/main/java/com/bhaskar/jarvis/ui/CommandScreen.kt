package com.bhaskar.jarvis.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bhaskar.jarvis.core.Command
import com.bhaskar.jarvis.core.CommandParser
import com.bhaskar.jarvis.core.ParseResult
import com.bhaskar.jarvis.core.StepResult
import com.bhaskar.jarvis.device.DeviceController
import com.bhaskar.jarvis.device.JarvisAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val COUNTDOWN_MS = 3_000L
private const val MAX_LOG_LINES = 50

/**
 * Typed command screen. Press Run, then switch to the app you want to control
 * during the countdown. Jarvis itself is never the target.
 */
@Composable
fun CommandScreen(
    controller: DeviceController,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var connected by remember { mutableStateOf(JarvisAccessibilityService.instance != null) }
    var input by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        while (true) {
            connected = JarvisAccessibilityService.instance != null
            delay(1_000)
        }
    }

    fun addLine(line: String) {
        log += line
        if (log.size > MAX_LOG_LINES) log.removeAt(0)
    }

    fun run() {
        val text = input
        running = true
        scope.launch {
            try {
                when (val parsed = CommandParser.parse(text)) {
                    is ParseResult.Rejected -> addLine("REJECTED: ${parsed.error.message}")
                    is ParseResult.Ok -> {
                        addLine("Switch to your target app. Starting in 3 seconds.")
                        delay(COUNTDOWN_MS)
                        val result = controller.run(parsed.command)
                        addLine(describe(parsed.command, result))
                    }
                }
            } finally {
                running = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Jarvis", style = MaterialTheme.typography.headlineMedium)

        Text(
            if (connected) {
                "Accessibility: on"
            } else {
                "Accessibility: off. Turn it on for Jarvis to control other apps."
            }
        )
        if (!connected) {
            Button(onClick = onOpenAccessibilitySettings) {
                Text("Open accessibility settings")
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("open <app>, tap <text>, or type <text>") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { run() },
            enabled = connected && !running && input.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (running) "Running..." else "Run in 3 seconds")
        }

        Text("Log", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(log) { line -> Text(line) }
        }
    }
}

/** Log text never includes typed content, only its length. */
private fun describe(command: Command, result: StepResult): String {
    val label = when (command) {
        is Command.OpenApp -> "open ${command.appName}"
        is Command.TapText -> "tap \"${command.visibleText}\""
        is Command.TypeText -> "type (${command.text.length} characters)"
    }
    return when (result) {
        is StepResult.Success -> "OK: $label. ${result.evidence}"
        is StepResult.Failure -> buildString {
            append("FAILED: $label. ${result.message}")
            if (result.candidates.isNotEmpty()) {
                append(" Options: ${result.candidates.joinToString()}")
            }
        }
    }
}
