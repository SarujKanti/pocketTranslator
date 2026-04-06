package com.skd.dictionary.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.skd.dictionary.R

class LanguageSpinnerAdapter(
    context: Context,
    private val items: List<LanguageItem>
) : ArrayAdapter<LanguageSpinnerAdapter.LanguageItem>(context, 0, items) {

    data class LanguageItem(
        val name: String,
        val flag: String,
        val nativeScript: String
    )

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_spinner_selected, parent, false)
        val item = items[position]
        view.findViewById<TextView>(R.id.tvFlag).text = item.flag
        view.findViewById<TextView>(R.id.tvLanguageName).text = item.name
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_spinner_dropdown, parent, false)
        val item = items[position]
        view.findViewById<TextView>(R.id.tvFlag).text = item.flag
        view.findViewById<TextView>(R.id.tvLanguageName).text = item.name
        view.findViewById<TextView>(R.id.tvNativeScript).text = item.nativeScript
        return view
    }
}
