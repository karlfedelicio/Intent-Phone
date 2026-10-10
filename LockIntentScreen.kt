package com.intentphone.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intentphone.launcher.data.validator.IntentionValidator
import com.intentphone.launcher.ui.theme.*

/**
 * Minimalist Intention Gate Screen with real-time text validation.
 * Psychological Principle: Think -> Write -> Clarify -> Commit -> Use phone.
 */
@Composable
fun LockIntentScreen(
    onCommitIntention: (intention: String, specificGoal: String) -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit
) {
    var intentionText by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val validation1 = remember(intentionText) { IntentionValidator.validateQuestion1(intentionText) }
    val validation2 = remember(goalText) { IntentionValidator.validateQuestion2(goalText) }
    val isFormValid = validation1.isValid && validation2.isValid
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenStats) {
                Icon(Icons.Default.Analytics, contentDescription = "Stats", tint = TextSecondaryDark)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondaryDark)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "What's your intention?",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Question 1
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Why did you unlock your phone?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = if (validation1.isValid) "${validation1.wordCount} / 5 words ✓" else "${validation1.wordCount} / 5 words",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (validation1.isValid) IntentSuccess else TextMutedDark
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = intentionText,
                onValueChange = { intentionText = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp).clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("Type your intention...", color = TextMutedDark) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (validation1.isValid) IntentSuccess else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (validation1.isValid) IntentSuccess.copy(alpha = 0.5f) else DarkBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            )
            if (validation1.message != null && intentionText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(validation1.message, style = MaterialTheme.typography.labelSmall, color = if (validation1.isMeaningless) IntentWarning else TextSecondaryDark)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("What exactly do you want to accomplish?", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = if (validation2.isValid) "${validation2.wordCount} / 5 words ✓" else "${validation2.wordCount} / 5 words",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (validation2.isValid) IntentSuccess else TextMutedDark
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp).clip(RoundedCornerShape(16.dp)),
                placeholder = { Text("What do you want to get done?", color = TextMutedDark) },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    if (isFormValid) onCommitIntention(intentionText.trim(), goalText.trim())
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (validation2.isValid) IntentSuccess else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (validation2.isValid) IntentSuccess.copy(alpha = 0.5f) else DarkBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            )
            if (validation2.message != null && goalText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(validation2.message, style = MaterialTheme.typography.labelSmall, color = if (validation2.isMeaningless) IntentWarning else TextSecondaryDark)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onCommitIntention(intentionText.trim(), goalText.trim())
                    }
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = AmoledBlack)
            ) {
                Text("Continue", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = onOpenEmergency, modifier = Modifier.height(36.dp)) {
                Text("Emergency Phone Call Only", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
            }
        }
    }
}