## General Guidelines

- always use the current local state of the code as base
- prefer to ask user, don't jump to conclusions
- ensure UTF-8 and CRLF line endings
- indent using 4 spaces
- prefer idiomatic code that is considered most concise and readable for the given language
- prefer readability and maintainabilty over compactness or performance
- consider performance and memory footprint for known problematic operations, but inform user first
- use human readable names for variables, parameters, properties etc.
- prefer named entities over implicit ones
- follow https://m3.material.io/ for the Android target
- use version catalog when adding dependencies and group the entries
- verify compilation using compile, but do not attempt time intensive builds or runs without explicit confirmation. When in doubt, request human review.

## Formatting

### Models/DTO

- each data class must be in its own `.kt` file, named after the class
- adhere to `.editorconfig`
- empty line between properties and above and below nested data classes
- trailing commas

### Composables / UI Components

- split composables by responsibility: if a component like `NowPlayingBar` is made up of distinct parts (e.g. `ProgressBar`, `PlaybackButtons`, `SongInfo`), each part gets its own
  `.kt` file rather than being nested inline inside `NowPlayingBar.kt`
- a component's own file should stay focused on composing its children, not implementing them
- extract a sub-component into its own file once it has its own distinct responsibility, state, or is non-trivial in size — trivial one-line wrappers (e.g. a styled `Spacer`) do not need to be split out
- when in doubt about whether to split, prefer splitting; ask the user only if the boundary is genuinely unclear

#### Screen-local vs. generic components

- distinguish between components reused across the app ("generic") and components that only make sense within a single screen ("screen-local")
- **generic components** (reusable across multiple screens) live in `ui/components/`
- **screen-local components** (used only within one screen) live in a `components/` folder nested under that screen's own package, e.g. `ui/screens/home/components/`
- every screen — including nested sub-screens — always gets its own `components/` folder as soon as it has at least one screen-local component, regardless of how few components it has
- shared utility composables (e.g. preview helpers) go in `ui/components/util/`
- **strict rule:** the moment a screen-local component is used by a second screen, it must be promoted to `ui/components/` immediately — never duplicated and never imported across screen packages, even temporarily
- follow this structure:

```
ui
├── components                     # generic, reused across screens
│   ├── ProductBlock.kt
│   ├── ScreenHeader.kt
│   └── util
│       └── MultiPreview.kt
├── screens
│   └── home
│       ├── components             # screen-local, only used within `home`
│       │   ├── HomeHeader.kt
│       │   └── QuickActions.kt
│       ├── HomeScreen.kt
│       └── locationpicker
│           ├── components         # screen-local to `locationpicker`, not shared with `home`
│           └── LocationPickerScreen.kt
```

### Extension functions

- placed under `xyz.skifty.moonlight.ext`
- file name must be `{ExtendedType}Ext.kt` (e.g. `StringExt.kt`, `ListExt.kt`, `MaterialThemeExt.kt`)
- follow the examples below

```
@Serializable
data class LoginRequest(
    @SerialName("email")
    val email: String,

    @SerialName("password")
    val password: String,
) {

    @Serializable
    data class Item(
        @SerialName("id")
        val id: String,

        @SerialName("name")
        val name: String? = null,
    )

}
```

### Function signatures / constructors

- put each parameter on its own line (no single-parameter exception)
- trailing comma after last parameter
- return type on its own line
- always specify return type unless it is Unit
- use `operator fun invoke` when applicable
- follow the examples below

```
@Composable
fun MainScreen(
    container: AppContainer = AppContainer(TokenStorage()),
) {
```

```
suspend fun getSiteDetails(
    siteId: Int,
): SiteDetails =
    client.get("${ApiConstants.API_PREFIX}/sites/$siteId/details")
        .body()
```

```
suspend operator fun invoke(
    siteId: Int,
): SiteDetails {
    if (siteId <= 0) {
        return SiteDetails()
    }
}
```

```
operator fun invoke(
    ids: List<Int>,
): List<Site> =
    ids
        .mapNotNull { id ->
            runCatching {
                getSite(id)
            }
                .getOrNull()
        }
```

```
fun String.toLocalizedTexts(): List<LocalizedText> =
    runCatching {
        lenientJson.decodeFromString<List<LocalizedText>>(this)
    }
        .getOrDefault(emptyList())
```

```
Column(
    modifier = Modifier.padding(
        horizontal = 16.dp,
        vertical = 10.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(4.dp),
```

### Lambdas and scope functions

- use named parameter instead of implicit `it` for all scope functions (`let`, `run`, `with`, `apply`, `also`)
- each statement in a lambda body on its own line with proper indentation
- chained calls after a lambda are indented +4 from the opening scope function
- follow the examples below

```
raw?.let { value ->
    runCatching {
        Instant.parse(value)
    }
        .getOrNull()
}
```

```
defaults.stringForKey(KEY_EXPIRES_AT)
    ?.let { raw ->
        runCatching {
            Instant.parse(raw)
        }
            .getOrNull()
    }
```

```
val sites = getSites()
    .filter { site ->
        site.shoppingEnabled > 0
    }
    .sortedBy { site ->
        site.name
    }
```

```
list.firstOrNull { text ->
    text.twoLetterIsoLanguageName.equals(languageCode, ignoreCase = true)
}
    ?.localizedTextValue
```

```
textStyle.copy(
    fontFamily = FontFamily.Default,
)
    .let { style ->
        style.copy(
            fontWeight = FontWeight.Bold,
        )
    }
```

### Null-safe calls (`?.`) and Elvis operator (`?:`)

- break after every `?.` and `?:` — each operator goes on its own line, indented +4 from the start of the preceding expression (same rule as chained calls after a lambda)
- the fallback value follows on the same line as `?:`
- follow the examples below

```
val token = getToken()
    ?: return false
```

```
val expiresAt = getExpiresAt()
    ?: return true
```

```
userLocation?.let { loc ->
    formatDistance(...)
}
    ?: "N/A"
```

```
name = cachedName
    ?: uiState.user
    ?.name
    ?: ""
```

### Coroutine builders (`launch`, `async`)

- always put the lambda body on its own line, indented +4 from the builder call
- never inline the body on the same line as `launch`/`async`
- follow the examples below

```
viewModelScope.launch {
    runCatching {
        authRepository.login(LoginRequest(...))
    }
}
```

```
val sitesDeferred = async {
    runCatching {
        siteRepository.getSites()
    }
}
```

### DI / Koin factory lambdas

- always put the lambda body on its own line, indented +4 from the opening brace
- never inline the body on the same line as `{`
- when constructing a class, use named parameters for all args (including single-arg cases for consistency within a module)
- `bind` — used to bind a `singleOf` definition to an interface type — is imported from `org.koin.dsl.bind`
- follow the examples below

```
single<AuthRepository> {
    AuthRepositoryImpl(
        api = get(),
        tokenStorage = get(),
    )
}
```

```
singleOf(::AuthRepositoryImpl) bind AuthRepository::class
```

```
viewModel { params ->
    PreOrderViewModel(
        siteRepository = get(),
        siteId = params.get(),
        allCategoryLabel = params.get(),
    )
}
```

### Conditional expressions (if/else)

- always use block body with braces and each branch on its own line
- never use the compact single-line form `if (x) y else z`
- keep the `if` on the same line as the assignment operator `=`
- follow the examples below

```
val colorScheme = if (darkTheme) {
    DarkColors
} else {
    LightColors
}
```

```
text = if (visible) {
    "Verbergen"
} else {
    "Anzeigen"
},
```

### when statements

- break after the arrow
- empty line each case
- follow the example below

```
fun NavTab.toTitle(): String =
    when (this) {
        NavTab.Start ->
            "Home"

        NavTab.Reservieren ->
            "Reservieren"

        NavTab.Bonus ->
            "Bonus"

        NavTab.Bestellungen ->
            "Bestellungen"

        NavTab.Profil ->
            "Profil"
    }
```

### Invocations

- put each parameter on its own line
- name each parameter in calls with 2+ parameters; single-parameter calls do not require named arguments (an unnamed single param may stay on one line, e.g. `Spacer(Modifier.height(8.dp))`); if a single param is named, place it on its own line
- Java and Objective-C interop methods do not support Kotlin named arguments; always use comment-style `/* param = */` for them
- use comment-style `/* param = */` for other cases where named parameters are unsupported (e.g. Java-generated synthetic accessors)
- trailing comma after last parameter
- data class `.copy()` calls follow the same rules as any other invocation
- follow the examples below

```
authRepository.login(
    LoginRequest(
        email = uiState.email,
        password = uiState.password,
    ),
)
```

```
LoginRoute(
    authRepository = container.authRepository,
    onLoginSuccess = {
        isLoggedIn = true
    },
)
```

```
uiState = uiState.copy(
    email = value,
    errorMessage = null,
)
```

```
Spacer(Modifier.height(8.dp))
```

```
Column(
    modifier = Modifier.weight(1f),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    // content
}
```

```
Log.d(
    /* tag = */ "Einkaufsbox-API",
    /* msg = */ message,
)
```

```
NSLog(
    /* format = */ "[Einkaufsbox-API] %@",
    message,
)
```

### Inline JSON / multiline string literals

- format inline JSON (arrays and objects) as raw multiline strings with proper indentation matching the surrounding code
- the triple-quote `"""` starts below the assignment (`=`)
- content inside is indented +8 relative to the variable declaration (i.e. one extra 4-space indent beyond the variable's own indentation)
- the closing `"""` is on its own line at the same indentation as the opening line
- follow the examples below

```
private val localizedProductJson =
    """
    [
        {
            "localizedTextValue": "Apfel",
            "twoLetterIsoLanguageName": "de"
        },
        {
            "localizedTextValue": "Apple",
            "twoLetterIsoLanguageName": "en"
        }
    ]
    """
```

### i18n / String Resources

- Use `stringResource(Res.string.xxx)` from `moonlight.shared.generated.resources` for all static UI strings, never hardcode text
- Use `pluralStringResource(Res.plurals.xxx, count, ...)` for plural strings (count selects form AND fills `%d`)
- Imports: import both `Res` AND each individual resource val, plus `stringResource`:

  ```
  import moonlight.shared.generated.resources.Res
  import moonlight.shared.generated.resources.login_username_label
  import org.jetbrains.compose.resources.stringResource
  ```

- Reference via `stringResource(Res.string.xxx)` — the individual imports ARE required (the Kotlin compiler cannot resolve extension properties on `Res.string` without them). Never use an imported val directly as a `StringResource` handle (i.e., don't pass it bare to a composable expecting `String`); always wrap in `stringResource()`.
- API-driven strings (product names, user names, site names, etc.) stay as-is — only static UI labels go through `stringResource`
- Validation error strings in ViewModels are passed as `String` parameters from the composable layer, never use `stringResource` directly in a ViewModel
- New strings go into `composeResources/values/strings.xml` (English, fallback), `composeResources/values-en/strings.xml` (English, explicit), and `composeResources/values-de/strings.xml` (German)
- For format args in `stringResource`, use `%1$s`, `%2$d` etc.; the first positional arg after the resource always maps to `%1$`

## Boundaries

Never modify this file without explicit authorization:

- *.md
- *.properties
- *.kts
- *.gitignore

Never perform any GIT operations without explicit authorization including checkouts and history scanning

## Verification

- after implementing a change, verifying compilation (per General Guidelines) is expected
- launching/building and running the app is fine, including navigating between screens (e.g. via deep links, test hooks, or normal in-app navigation) to reproduce issues or reach a particular state
- checking logs, crash output, and runtime errors from a running app is fine
- **never** control the mouse or keyboard to interact with a running app (clicking, tapping, scrolling, typing into fields, etc.)
- **never** take or inspect screenshots to visually confirm UI changes (layout, spacing, colors, alignment, rendering correctness, etc.)
- visual/UI confirmation is the user's responsibility — after making a UI change, briefly describe what was changed and ask the user to confirm it looks correct
- if a change cannot be verified through compilation, logs, or navigation alone, say so explicitly instead of attempting to confirm it visually
