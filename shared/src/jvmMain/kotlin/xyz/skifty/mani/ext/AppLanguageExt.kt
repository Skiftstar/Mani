package xyz.skifty.mani.ext

import xyz.skifty.mani.i18n.AppLanguage
import java.util.Locale

/** Resolves [AppLanguage.SYSTEM] back to whatever the OS locale actually was at startup. */
fun AppLanguage.toLocale(systemDefault: Locale): Locale =
    languageTag?.let { Locale.forLanguageTag(it) } ?: systemDefault
