package com.skd.dictionary.utils

import com.google.mlkit.nl.translate.TranslateLanguage

object LanguageConstants {

    val indianLanguages = mapOf(
        "Hindi" to TranslateLanguage.HINDI,
        "Tamil" to TranslateLanguage.TAMIL,
        "Telugu" to TranslateLanguage.TELUGU,
        "Kannada" to TranslateLanguage.KANNADA,
        "Bengali" to TranslateLanguage.BENGALI,
        "Marathi" to TranslateLanguage.MARATHI,
        "Gujarati" to TranslateLanguage.GUJARATI,
        "Urdu" to TranslateLanguage.URDU
    )

     val globalLanguages = mapOf(
        "English" to TranslateLanguage.ENGLISH,
        "Spanish" to TranslateLanguage.SPANISH,
        "French" to TranslateLanguage.FRENCH,
        "German" to TranslateLanguage.GERMAN,
        "Chinese" to TranslateLanguage.CHINESE,
        "Japanese" to TranslateLanguage.JAPANESE,
        "Russian" to TranslateLanguage.RUSSIAN,
        "Portuguese" to TranslateLanguage.PORTUGUESE
    )
}