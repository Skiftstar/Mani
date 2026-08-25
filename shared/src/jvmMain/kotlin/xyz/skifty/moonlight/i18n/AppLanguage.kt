package xyz.skifty.moonlight.i18n

/**
 * Languages the desktop UI can be shown in. [languageTag] is a BCP-47 tag matching a
 * composeResources qualifier folder (e.g. "de" -> composeResources/values-de); null means "follow
 * the OS locale". [displayName] is each language's own name, deliberately not translated (a
 * language picker conventionally shows entries in their own language, not the currently active one).
 *
 * There's no public API in Compose Multiplatform 1.9.3 to override a composition's resource
 * language directly (`LocalComposeEnvironment`/`ComposeEnvironment`/`ResourceEnvironment` all
 * turned out to be `internal` to the resources library, despite looking public from the compiled
 * bytecode alone). Compose resources instead resolve the active language from
 * `java.util.Locale.getDefault()` fresh on every lookup - a real JDK API - so switching languages
 * here means calling [java.util.Locale.setDefault] and then forcing recomposition (see
 * [xyz.skifty.moonlight.ext.toLocale] and the `key(appLanguage)` wrapper around the app content
 * in JvmApp.kt).
 */
enum class AppLanguage(val languageTag: String?, val displayName: String) {
    SYSTEM(null, "System"),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
}
