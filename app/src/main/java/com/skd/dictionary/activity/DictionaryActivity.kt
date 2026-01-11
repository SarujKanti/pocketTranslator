package com.skd.dictionary.activity

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.nl.translate.TranslateLanguage
import com.skd.dictionary.R
import com.skd.dictionary.dataModel.WordDetailItem
import com.skd.dictionary.utils.LanguageConstants
import com.skd.dictionary.utils.TranslatorHelper
import com.skd.dictionary.viewModel.DictionaryViewModel
import java.util.Locale
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skd.dictionary.adapter.WordDetailAdapter

class DictionaryActivity : AppCompatActivity() {
    private lateinit var translatorHelper: TranslatorHelper
    private lateinit var etInput: EditText
    private lateinit var tvResult: TextView
    private lateinit var btnTranslate: Button
    private lateinit var spinnerLanguage: Spinner
    private lateinit var tts: TextToSpeech
    private lateinit var btnSpeak: ImageButton
    private lateinit var btnClear: ImageButton

    private lateinit var dictionaryViewModel: DictionaryViewModel
    private lateinit var rvWordDetails: RecyclerView
    private lateinit var wordDetailAdapter: WordDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        translatorHelper = TranslatorHelper()
        toolbar()
        initViews()
        dictionaryViewModel = ViewModelProvider(this)[DictionaryViewModel::class.java]
        observeDictionaryData()
        setupLanguageSpinner()
        setupTranslateAction()
        preloadLanguages()
    }

    private fun toolbar(){
        setContentView(R.layout.activity_dictionary)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observeDictionaryData() {

        dictionaryViewModel.wordDetails.observe(this) { responseList ->

            val items = mutableListOf<WordDetailItem>()

            responseList.forEach { response ->
                response.meanings?.forEach { meaning ->
                    meaning.definitions?.forEach { def ->

                        // Definition
                        def.definition?.let {
                            items.add(
                                WordDetailItem(
                                    title = meaning.partOfSpeech?.uppercase() ?: "Definition",
                                    description = it
                                )
                            )
                        }

                        // Example
                        def.example?.let {
                            items.add(
                                WordDetailItem("Example", it)
                            )
                        }

                        // Antonyms
                        def.antonyms?.takeIf { it.isNotEmpty() }?.let {
                            items.add(
                                WordDetailItem("Antonyms", it.joinToString(", "))
                            )
                        }
                    }
                }
            }

            wordDetailAdapter.updateList(items)
        }

        dictionaryViewModel.error.observe(this) { errorMsg ->
            // Show error elsewhere (Toast or Snackbar)
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }



    private fun preloadLanguages() {
        listOf(
            TranslateLanguage.HINDI,
            TranslateLanguage.TAMIL,
            TranslateLanguage.TELUGU
        ).forEach { lang ->
            translatorHelper.translate(
                text = "Hello",
                targetLanguage = lang,
                onDownloading = {},
                onSuccess = {},
                onError = {}
            )
        }
    }


    private fun initViews() {
        etInput = findViewById(R.id.etInput)
        tvResult = findViewById(R.id.tvResult)
        btnTranslate = findViewById(R.id.btnTranslate)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        btnSpeak = findViewById(R.id.btnSpeak)
        btnClear = findViewById(R.id.btnClear)

        rvWordDetails = findViewById(R.id.rvWordDetails)
        rvWordDetails.layoutManager = LinearLayoutManager(this)

        wordDetailAdapter = WordDetailAdapter(mutableListOf())
        rvWordDetails.adapter = wordDetailAdapter

        initTextToSpeech()
        btnClear.setOnClickListener {
            tvResult.text = ""
        }

        btnSpeak.setOnClickListener {

            val textToSpeak = tvResult.text.toString().trim()

            if (textToSpeak.isEmpty()) {
                tvResult.text = "Nothing to pronounce"
                return@setOnClickListener
            }

            val selectedLanguage =
                spinnerLanguage.selectedItem as? String ?: return@setOnClickListener

            val locale = getLocaleForLanguage(selectedLanguage)

            val result = tts.setLanguage(locale)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tvResult.text = "Voice not supported for $selectedLanguage"
            } else {
                tts.speak(
                    textToSpeak,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    null
                )
            }
        }


    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US // default (English)
            }
        }
    }


    private fun setupLanguageSpinner() {

        val languageNames = LanguageConstants.indianLanguages.keys.toList()

        spinnerLanguage.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languageNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLanguage =
                        parent.getItemAtPosition(position) as String

                    val targetLangCode =
                        LanguageConstants.indianLanguages[selectedLanguage]
                            ?: return

                    // Pre-download model when language is selected
                    translatorHelper.translate(
                        text = "Hello",
                        targetLanguage = targetLangCode,
                        onDownloading = {
                            tvResult.text = "Preparing $selectedLanguage…"
                        },
                        onSuccess = {},
                        onError = {}
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }


    private fun setupTranslateAction() {
        btnTranslate.setOnClickListener {

            val text = etInput.text.toString().trim()

            if (text.isEmpty()) {
                tvResult.text = "Please enter text"
                return@setOnClickListener
            }

            // 🔹 ALWAYS fetch dictionary data
            dictionaryViewModel.getWordDetails(text)

            val selectedLanguage = spinnerLanguage.selectedItem as? String ?: return@setOnClickListener
            val targetLangCode =
                LanguageConstants.indianLanguages[selectedLanguage] ?: return@setOnClickListener

            tvResult.text = "Preparing translation…"

            translatorHelper.translate(
                text = text,
                targetLanguage = targetLangCode,
                onDownloading = {
                    tvResult.text = "Searching…"
                },
                onSuccess = { translated ->
                    tvResult.text = translated
                },
                onError = {
                    tvResult.text = "Translation failed"
                }
            )
        }
    }

    private fun getLocaleForLanguage(language: String): Locale {
        return when (language) {
            "Hindi" -> Locale("hi", "IN")
            "Tamil" -> Locale("ta", "IN")
            "Telugu" -> Locale("te", "IN")
            "Kannada" -> Locale("kn", "IN")
            "Bengali" -> Locale("bn", "IN")
            "Marathi" -> Locale("mr", "IN")
            "Gujarati" -> Locale("gu", "IN")
            "Urdu" -> Locale("ur", "IN")
            else -> Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }

}