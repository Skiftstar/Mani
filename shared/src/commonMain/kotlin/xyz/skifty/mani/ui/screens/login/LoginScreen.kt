package xyz.skifty.mani.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mani.shared.generated.resources.Res
import mani.shared.generated.resources.cd_hide_password
import mani.shared.generated.resources.cd_show_password
import mani.shared.generated.resources.login_button
import mani.shared.generated.resources.login_failed
import mani.shared.generated.resources.login_password_label
import mani.shared.generated.resources.login_save_failed
import mani.shared.generated.resources.login_server_url_label
import mani.shared.generated.resources.login_title
import mani.shared.generated.resources.login_username_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import xyz.skifty.mani.api.ApiService
import xyz.skifty.mani.ext.trackTextFieldFocus
import xyz.skifty.mani.security.SecureStorage

/** A login-screen error that hasn't been resolved to displayable text yet - some variants need
 *  [stringResource] (and so must be resolved during composition, not from the coroutine that
 *  raises them), others carry text that's already final (e.g. an exception's own message). */
private sealed interface LoginUiError {
    data class Message(val text: String) : LoginUiError
    data object LoginFailed : LoginUiError
    data class SaveFailed(val detail: String) : LoginUiError
}

// More rounded than OutlinedTextField's default (a much subtler MaterialTheme.shapes.extraSmall),
// but well short of a fully-rounded pill - just softened corners on the three fields below.
private val LoginFieldShape = RoundedCornerShape(16.dp)

@Composable
fun LoginScreen(apiService: ApiService, onLoginSuccess: () -> Unit) {

    val secureStorage = koinInject<SecureStorage>()
    val scope = rememberCoroutineScope()

    var apiUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var uiError by remember { mutableStateOf<LoginUiError?>(null) }

    LaunchedEffect(Unit) {
        try {
            secureStorage.get("mani_api_url")
                ?.let { apiUrl = it }
            secureStorage.get("mani_username")
                ?.let { username = it }
        } catch (e: Exception) {
            // No saved values to prefill is fine - just leave the fields blank.
            System.err.println("Could not read saved login details: ${e.message}")
        }
    }

    fun submit() {
        uiError = null
        isLoading = true
        scope.launch {
            apiService.configure(apiUrl, username, password)
            val result = apiService.ping()
            result.fold(
                onSuccess = {
                    apiService.currentSession?.let { s ->
                        try {
                            secureStorage.save("mani_api_url", s.apiUrl)
                            secureStorage.save("mani_username", s.username)
                            secureStorage.save("mani_token", s.token)
                            secureStorage.save("mani_salt", s.salt)
                        } catch (e: Exception) {
                            // Login itself succeeded - don't block the user over this,
                            // just let them know they'll need to log in again next time.
                            uiError = LoginUiError.SaveFailed(e.message ?: "")
                        }
                    }
                    isLoading = false
                    onLoginSuccess()
                },
                onFailure = { e ->
                    uiError =
                        e.message?.let { LoginUiError.Message(it) } ?: LoginUiError.LoginFailed
                    isLoading = false
                },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // No card - unlike the previous design, this one sits directly on the screen background,
        // full-bleed and left-aligned, matching the reference login screen the user provided.
        Column(
            modifier = Modifier.widthIn(max = 400.dp)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(Res.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            OutlinedTextField(
                value = apiUrl,
                onValueChange = { apiUrl = it },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
                    .trackTextFieldFocus(),
                label = { Text(stringResource(Res.string.login_server_url_label)) },
                placeholder = { Text("https://your-navidrome-server.com") },
                shape = LoginFieldShape,
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
                    .trackTextFieldFocus(),
                label = { Text(stringResource(Res.string.login_username_label)) },
                shape = LoginFieldShape,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
                    .trackTextFieldFocus(),
                label = { Text(stringResource(Res.string.login_password_label)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) Res.string.cd_hide_password else Res.string.cd_show_password,
                            ),
                        )
                    }
                },
                shape = LoginFieldShape,
            )

            uiError?.let { err ->
                val text = when (err) {
                    is LoginUiError.Message -> err.text
                    LoginUiError.LoginFailed -> stringResource(Res.string.login_failed)
                    is LoginUiError.SaveFailed -> stringResource(
                        Res.string.login_save_failed,
                        err.detail,
                    )
                }
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = { submit() },
                enabled = !isLoading && apiUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(Res.string.login_button))
                }
            }
        }
    }
}
