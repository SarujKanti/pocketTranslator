package com.skd.dictionary.activity

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.nl.translate.TranslateLanguage
import com.skd.dictionary.R
import com.skd.dictionary.adapter.WordDetailAdapter
import com.skd.dictionary.constant.StringConstant
import com.skd.dictionary.dataModel.WordDetailItem
import com.skd.dictionary.utils.LanguageConstants
import com.skd.dictionary.utils.TranslatorHelper
import com.skd.dictionary.viewModel.DictionaryViewModel
import java.util.Locale

class DictionaryActivity : AppCompatActivity() {
    private lateinit var translatorHelper: TranslatorHelper
    private lateinit var etInput: EditText
    private lateinit var tvResult: TextView
    private lateinit var btnTranslate: Button
    private lateinit var spinnerLanguage: Spinner
    private lateinit var tts: TextToSpeech
    private lateinit var btnSpeak: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var progressWordDetails: ProgressBar
    private lateinit var dictionaryViewModel: DictionaryViewModel
    private lateinit var rvWordDetails: RecyclerView
    private lateinit var wordDetailAdapter: WordDetailAdapter
    private lateinit var btnSpeakInput: ImageButton
    private lateinit var btnClearInput: ImageButton

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
            progressWordDetails.visibility = View.GONE
            rvWordDetails.visibility = View.VISIBLE
            val items = mutableListOf<WordDetailItem>()

            responseList.forEach { response ->
                response.meanings?.forEach { meaning ->
                    meaning.definitions?.forEach { def ->

                        def.definition?.let {
                            items.add(
                                WordDetailItem(
                                    title = meaning.partOfSpeech?.uppercase() ?: "Definition",
                                    description = it
                                )
                            )
                        }

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
            progressWordDetails.visibility = View.GONE

            rvWordDetails.visibility = View.GONE

            Toast.makeText(this, errorMsg ?: getString(R.string.details_not_found), Toast.LENGTH_SHORT).show()
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
        progressWordDetails = findViewById(R.id.progressWordDetails)
        rvWordDetails = findViewById(R.id.rvWordDetails)
        btnSpeakInput = findViewById(R.id.btnSpeakInput)
        btnClearInput = findViewById(R.id.btnClearInput)
        rvWordDetails.layoutManager = LinearLayoutManager(this)

        wordDetailAdapter = WordDetailAdapter(mutableListOf())
        rvWordDetails.adapter = wordDetailAdapter

        initTextToSpeech()
        btnClear.setOnClickListener {
            tvResult.text = ""
        }

        btnClearInput.setOnClickListener {
            // Clear input text
            etInput.text.clear()

            // Clear result text
            tvResult.text = ""

            // Clear RecyclerView data
            wordDetailAdapter.updateList(emptyList())

            // Optional: hide RecyclerView
            rvWordDetails.visibility = View.GONE
        }


        btnSpeakInput.setOnClickListener {

            val textToSpeak = etInput.text.toString().trim()

            if (textToSpeak.isEmpty()) {
                Toast.makeText(this, getString(R.string.nothing_to_speak), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedLanguage =
                spinnerLanguage.selectedItem as? String ?: return@setOnClickListener

            val locale = getLocaleForLanguage(selectedLanguage)

            val result = tts.setLanguage(locale)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Toast.makeText(
                    this,
                    "Voice not supported for $selectedLanguage",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                tts.speak(
                    textToSpeak,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "INPUT_SPEAK"
                )
            }
        }


        btnSpeak.setOnClickListener {

            val textToSpeak = tvResult.text.toString().trim()

            if (textToSpeak.isEmpty()) {
                tvResult.text = getString(R.string.nothing_to_pronounce)
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

//                    val targetLangCode = LanguageConstants.indianLanguages[selectedLanguage] ?: return
                    // Pre-download model when language is selected
//                    translatorHelper.translate(
//                        text = "Hello",
//                        targetLanguage = targetLangCode,
//                        onDownloading = {},
//                        onSuccess = {},
//                        onError = {}
//                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun setupTranslateAction() {
        btnTranslate.setOnClickListener {
            hideKeyboard()

            val word = etInput.text.toString().trim()
            if (word.isEmpty()) {
                tvResult.text = getString(R.string.please_enter_word)
                return@setOnClickListener
            }

            // Dictionary call (English meaning)
            progressWordDetails.visibility = View.VISIBLE
            rvWordDetails.visibility = View.INVISIBLE
            dictionaryViewModel.getWordDetails(word)

            // Translate entered word
            val selectedLanguage = spinnerLanguage.selectedItem as String
            val targetLanguage =
                LanguageConstants.indianLanguages[selectedLanguage]
                    ?: return@setOnClickListener

            translatorHelper.translate(
                text = word,
                targetLanguage = targetLanguage,
                onDownloading = {
                    tvResult.text = getString(R.string.translating)
                },
                onSuccess = { translatedText ->
                    tvResult.text = translatedText
                },
                onError = {
                    tvResult.text = getString(R.string.failed_translating)
                }
            )
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etInput.windowToken, 0)
    }

    private fun getLocaleForLanguage(language: String): Locale {
        return when (language) {
            StringConstant.HINDI -> Locale(StringConstant.SHORT_HINDI, StringConstant.INDIA)
            StringConstant.TAMIL -> Locale(StringConstant.SHORT_TAMIL, StringConstant.INDIA)
            StringConstant.TELUGU -> Locale(StringConstant.SHORT_TELUGU, StringConstant.INDIA)
            StringConstant.KANNADA -> Locale(StringConstant.SHORT_KANNADA, StringConstant.INDIA)
            StringConstant.BENGALI -> Locale(StringConstant.SHORT_BENGALI, StringConstant.INDIA)
            StringConstant.MARATHI -> Locale(StringConstant.SHORT_MARATHI, StringConstant.INDIA)
            StringConstant.GUJARATI -> Locale(StringConstant.SHORT_GUJARATI, StringConstant.INDIA)
            StringConstant.URDU -> Locale(StringConstant.SHORT_URDU, StringConstant.INDIA)
            else -> Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        translatorHelper.closeAll()
    }


}