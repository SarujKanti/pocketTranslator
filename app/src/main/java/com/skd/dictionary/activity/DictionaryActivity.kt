package com.skd.dictionary.activity

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.skd.dictionary.R
import com.skd.dictionary.adapter.LanguageSpinnerAdapter
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
    private var isTtsReady = false
    private lateinit var btnSpeak: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var progressWordDetails: ProgressBar
    private lateinit var dictionaryViewModel: DictionaryViewModel
    private lateinit var rvWordDetails: RecyclerView
    private lateinit var wordDetailAdapter: WordDetailAdapter
    private lateinit var btnSpeakInput: ImageButton
    private lateinit var btnClearInput: ImageButton
    private lateinit var tvPronunciation: TextView
    private lateinit var progressTranslation: ProgressBar

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

        val englishOnlyFilter = InputFilter { source, start, end, _, _, _ ->
            val filtered = StringBuilder()
            for (i in start until end) {
                val c = source[i]
                if (c in 'a'..'z' || c in 'A'..'Z' || c == ' ') {
                    filtered.append(c)
                }
            }
            // Return null (accept unchanged) if all chars are valid; otherwise return filtered string
            if (filtered.length == end - start) null else filtered
        }

        etInput.filters = arrayOf(englishOnlyFilter)

        // Second-layer guard: strips any non-English characters that bypass the
        // InputFilter (e.g. clipboard paste on some Android versions).
        etInput.addTextChangedListener(object : TextWatcher {
            private var isCleaning = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isCleaning || s == null) return
                val cleaned = s.toString().replace(Regex("[^a-zA-Z ]"), "")
                if (cleaned != s.toString()) {
                    isCleaning = true
                    s.replace(0, s.length, cleaned)
                    isCleaning = false
                }
            }
        })
    }

    private fun toolbar() {
        setContentView(R.layout.activity_dictionary)

        // White icons on the dark gradient — works API 23+ via WindowInsetsControllerCompat
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false   // white clock/battery/signal icons
            isAppearanceLightNavigationBars = true // dark nav-bar icons on the light bg_page
        }

        // Expand viewHeader to cover the status bar so the gradient flows
        // seamlessly behind the clock/battery row on every Android version.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewHeader)) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            // Push tvTitle / cardInfoLogo below the status bar
            view.setPadding(0, statusBarHeight, 0, 0)
            // Expand the header height to include the status bar area
            val visualHeight = (80 * resources.displayMetrics.density).toInt()
            view.layoutParams.height = statusBarHeight + visualHeight
            view.requestLayout()
            insets
        }

        // Prevent scroll content from being hidden behind the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollContent)) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(0, 0, 0, navBar.bottom)
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

                        def.synonyms?.takeIf { it.isNotEmpty() }?.let {
                            items.add(WordDetailItem("Synonyms", it.joinToString(", ")))
                        }

                        def.antonyms?.takeIf { it.isNotEmpty() }?.let {
                            items.add(WordDetailItem("Antonyms", it.joinToString(", ")))
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
        // Download all 8 language models in the background at startup so
        // every language translates instantly when the user picks it.
        LanguageConstants.indianLanguages.values.forEach { langCode ->
            translatorHelper.downloadModel(langCode)
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
        tvPronunciation = findViewById(R.id.tvPronunciation)
        progressTranslation = findViewById(R.id.progressTranslation)
        rvWordDetails.layoutManager = LinearLayoutManager(this)
        val ivInfoLogo: ImageView = findViewById(R.id.ivInfoLogo)

        wordDetailAdapter = WordDetailAdapter(mutableListOf())
        rvWordDetails.adapter = wordDetailAdapter

        initTextToSpeech()
        btnClear.setOnClickListener {
            tvResult.text = ""
            tvPronunciation.text = ""
            tvPronunciation.visibility = View.GONE
        }

        ivInfoLogo.setOnClickListener {
            showAppInfoDialog()
        }

        btnClearInput.setOnClickListener {
            etInput.text.clear()
            tvResult.text = ""
            tvPronunciation.text = ""
            tvPronunciation.visibility = View.GONE
            wordDetailAdapter.updateList(emptyList())
            rvWordDetails.visibility = View.GONE
        }


        btnSpeakInput.setOnClickListener {
            val textToSpeak = etInput.text.toString().trim()

            if (textToSpeak.isEmpty()) {
                Toast.makeText(this, getString(R.string.nothing_to_speak), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isTtsReady) {
                Toast.makeText(this, "Speech engine is not ready yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Input is always English — use English locale
            tts.setLanguage(Locale.US)
            tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "INPUT_SPEAK")
        }


        btnSpeak.setOnClickListener {
            val textToSpeak = tvResult.text.toString().trim()

            if (textToSpeak.isEmpty()) {
                Toast.makeText(this, getString(R.string.nothing_to_pronounce), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isTtsReady) {
                Toast.makeText(this, "Speech engine is not ready yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedLanguage =
                (spinnerLanguage.selectedItem as? LanguageSpinnerAdapter.LanguageItem)?.name
                    ?: return@setOnClickListener

            val locale = getLocaleForLanguage(selectedLanguage)
            val result = tts.setLanguage(locale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Voice not supported for $selectedLanguage", Toast.LENGTH_SHORT).show()
            } else {
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }


    }

    private fun showAppInfoDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_app_info)
        dialog.setCancelable(true)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()

        val window = dialog.window ?: return
        val params = window.attributes

        val marginInDp = 20
        val density = resources.displayMetrics.density
        val marginInPx = (marginInDp * density).toInt()

        params.width = resources.displayMetrics.widthPixels - (marginInPx * 2)
        window.attributes = params
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US // default (English)
                isTtsReady = true
            }
        }
    }


    private fun setupLanguageSpinner() {
        val languageItems = listOf(
            LanguageSpinnerAdapter.LanguageItem("Hindi",    "🇮🇳", "हिंदी"),
            LanguageSpinnerAdapter.LanguageItem("Tamil",    "🇮🇳", "தமிழ்"),
            LanguageSpinnerAdapter.LanguageItem("Telugu",   "🇮🇳", "తెలుగు"),
            LanguageSpinnerAdapter.LanguageItem("Kannada",  "🇮🇳", "ಕನ್ನಡ"),
            LanguageSpinnerAdapter.LanguageItem("Bengali",  "🇮🇳", "বাংলা"),
            LanguageSpinnerAdapter.LanguageItem("Marathi",  "🇮🇳", "मराठी"),
            LanguageSpinnerAdapter.LanguageItem("Gujarati", "🇮🇳", "ગુજરાતી"),
            LanguageSpinnerAdapter.LanguageItem("Urdu",     "🇮🇳", "اردو")
        )

        spinnerLanguage.adapter = LanguageSpinnerAdapter(this, languageItems)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupTranslateAction() {
        btnTranslate.setOnClickListener {
            hideKeyboard()

            val word = etInput.text.toString().trim()
            if (word.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_word), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Dictionary lookup
            progressWordDetails.visibility = View.VISIBLE
            rvWordDetails.visibility = View.INVISIBLE
            dictionaryViewModel.getWordDetails(word)

            // Show translation in-flight state
            progressTranslation.visibility = View.VISIBLE
            btnTranslate.isEnabled = false

            val selectedLanguage =
                (spinnerLanguage.selectedItem as? LanguageSpinnerAdapter.LanguageItem)?.name ?: ""
            val targetLanguage =
                LanguageConstants.indianLanguages[selectedLanguage]
                    ?: run {
                        progressTranslation.visibility = View.GONE
                        btnTranslate.isEnabled = true
                        return@setOnClickListener
                    }

            translatorHelper.translate(
                text = word,
                targetLanguage = targetLanguage,
                onDownloading = {},
                onSuccess = { translatedText ->
                    progressTranslation.visibility = View.GONE
                    btnTranslate.isEnabled = true
                    tvResult.text = translatedText
                    val pronunciation = romanize(translatedText)
                    if (pronunciation.isNotBlank() && pronunciation != translatedText) {
                        tvPronunciation.text = pronunciation
                        tvPronunciation.visibility = View.VISIBLE
                    } else {
                        tvPronunciation.visibility = View.GONE
                    }
                },
                onError = {
                    progressTranslation.visibility = View.GONE
                    btnTranslate.isEnabled = true
                    Toast.makeText(this, getString(R.string.failed_translating), Toast.LENGTH_SHORT).show()
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

    private fun romanize(text: String): String {
        return try {
            // Any-Latin converts any Unicode script → Latin; NFD+Mark removal strips diacritics
            val transliterator = android.icu.text.Transliterator
                .getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")
            transliterator.transliterate(text)
        } catch (e: Exception) {
            ""
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