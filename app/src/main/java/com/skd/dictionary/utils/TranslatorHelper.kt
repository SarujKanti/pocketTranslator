package com.skd.dictionary.utils

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.*

class TranslatorHelper {

    private val translators = mutableMapOf<String, Translator>()

    fun translate(
        text: String,
        targetLanguage: String,
        onDownloading: () -> Unit,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val translator = translators.getOrPut(targetLanguage) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }
        val conditions = DownloadConditions.Builder().build()

        onDownloading()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }
}
