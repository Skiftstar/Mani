package xyz.skifty.moonlight.ext

import xyz.skifty.moonlight.i18n.AppLanguage
import java.util.Locale

/** Resolves [AppLanguage.SYSTEM] back to whatever the OS locale actually was at startup. */
fun AppLanguage.toLocale(systemDefault: Locale): Locale =
    languageTag?.let { Locale.forLanguageTag(it) } ?: systemDefault
