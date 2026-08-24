package xyz.skifty.moonlight.screens

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
import xyz.skifty.moonlight.api.ApiService
import xyz.skifty.moonlight.security.SecureStorageFactory

@Composable
fun LoginScreen(apiService: ApiService, onLoginSuccess: () -> Unit) {

    val secureStorage = remember { SecureStorageFactory.create() }
    val scope = rememberCoroutineScope()

    var apiUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        secureStorage.get("moonlight_api_url")?.let { apiUrl = it }
        secureStorage.get("moonlight_username")?.let { username = it }
    }

    fun submit() {
        errorMessage = null
        isLoading = true
        scope.launch {
            apiService.configure(apiUrl, username, password)
            val result = apiService.ping()
            result.fold(
                onSuccess = {
                    apiService.currentSession?.let { s ->
                        secureStorage.save("moonlight_api_url", s.apiUrl)
                        secureStorage.save("moonlight_username", s.username)
                        secureStorage.save("moonlight_token", s.token)
                        secureStorage.save("moonlight_salt", s.salt)
                    }
                    isLoading = false
                    onLoginSuccess()
                },
                onFailure = { e ->
                    errorMessage = e.message ?: "Login failed"
                    isLoading = false
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.widthIn(max = 400.dp).padding(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Moonlight", style = MaterialTheme.typography.headlineMedium)
                Text("Sign in to your Navidrome server", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server URL") },
                    placeholder = { Text("https://your-navidrome-server.com") }
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    }
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = { submit() },
                    enabled = !isLoading && apiUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Log In")
                    }
                }
            }
        }
    }
}
