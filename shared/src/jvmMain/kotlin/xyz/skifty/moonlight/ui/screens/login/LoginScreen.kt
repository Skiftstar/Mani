package xyz.skifty.moonlight.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moonlight.shared.generated.resources.Res
import moonlight.shared.generated.resources.app_name
import moonlight.shared.generated.resources.cd_hide_password
import moonlight.shared.generated.resources.cd_show_password
import moonlight.shared.generated.resources.login_button
import moonlight.shared.generated.resources.login_failed
import moonlight.shared.generated.resources.login_password_label
import moonlight.shared.generated.resources.login_save_failed
import moonlight.shared.generated.resources.login_server_url_label
import moonlight.shared.generated.resources.login_subtitle
import moonlight.shared.generated.resources.login_username_label
import org.jetbrains.compose.resources.stringResource
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.security.SecureStorageFactory

/** A login-screen error that hasn't been resolved to displayable text yet - some variants need
 *  [stringResource] (and so must be resolved during composition, not from the coroutine that
 *  raises them), others carry text that's already final (e.g. an exception's own message). */
private sealed interface LoginUiError {
    data class Message(val text: String) : LoginUiError
    data object LoginFailed : LoginUiError
    data class SaveFailed(val detail: String) : LoginUiError
}

@Composable
fun LoginScreen(apiService: ApiService, onLoginSuccess: () -> Unit) {

    val secureStorage = remember { SecureStorageFactory.create() }
    val scope = rememberCoroutineScope()

    var apiUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var uiError by remember { mutableStateOf<LoginUiError?>(null) }

    LaunchedEffect(Unit) {
        try {
            secureStorage.get("moonlight_api_url")
                ?.let { apiUrl = it }
            secureStorage.get("moonlight_username")
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
                            secureStorage.save("moonlight_api_url", s.apiUrl)
                            secureStorage.save("moonlight_username", s.username)
                            secureStorage.save("moonlight_token", s.token)
                            secureStorage.save("moonlight_salt", s.salt)
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.widthIn(max = 400.dp)
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(Res.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                )

                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.login_server_url_label)) },
                    placeholder = { Text("https://your-navidrome-server.com") },
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.login_username_label)) },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
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
}
