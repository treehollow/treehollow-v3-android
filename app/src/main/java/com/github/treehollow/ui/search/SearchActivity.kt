package com.github.treehollow.ui.search

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.base.Event
import com.github.treehollow.databinding.ActivitySearchBinding
import com.github.treehollow.repository.SearchParameter
import com.github.treehollow.utils.Utils
import com.microsoft.appcenter.analytics.Analytics
import com.skydoves.bundler.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate.*
import java.util.*


class SearchActivity : DataBindingActivity() {

    @VisibleForTesting
    private val initKeywords: String by bundleNonNull("keywords")
    val model: SearchViewModel by viewModels()
    private val binding: ActivitySearchBinding by binding(R.layout.activity_search)

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(this)
        super.onCreate(savedInstanceState)
        binding.apply {
            lifecycleOwner = this@SearchActivity
            vm = model
        }
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        model.errorMsg.observe(this,
            { event ->
                event?.getContentIfNotHandledOrReturnNull()?.let {
                    Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                    Log.e("SearchActivity", it)
                }
            })

        model.infoMsg.observe(this,
            { event ->
                event?.getContentIfNotHandledOrReturnNull()?.let {
                    Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
                    Log.e("SearchActivity", it)
                }
            })

        val searchButton = binding.searchButton

        searchButton.setOnClickListener {
            doSearch()
        }
        binding.searchKeywordsEditText.setText(initKeywords)
        binding.searchKeywordsEditText.setOnKeyListener { _, keyCode, event ->
            if ((event.action == KeyEvent.ACTION_DOWN) &&
                (keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                doSearch()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

        val isAdvance = model.isAdvanceSearchDisplay
        binding.advancedSearch.setOnClickListener {
            isAdvance.value?.let { value ->
                isAdvance.value = !value
            }
        }

        binding.buttonPostOnly.setOnClickListener {
            setIncludeCommentView(false)
        }
        binding.buttonIncludeComment.setOnClickListener {
            setIncludeCommentView(true)
        }
        setIncludeCommentView(true)

        binding.searchKeywordsEditText.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    fun doSearch() {
        val searchKeyWords = binding.searchKeywordsTextField
//        val searchButton = binding.searchButton
        val startDate = binding.startDate
        val endDate = binding.endDate
        val keywords = searchKeyWords.editText?.text.toString()
        val before: Long = dateToUnixTimestamp(endDate.text.toString()) ?: -1
        val after: Long = dateToUnixTimestamp(startDate.text.toString()) ?: -1
        val includeComment = model.isIncludeComment.value ?: false
        val order = "id"

        if (keywords != "") {
            val properties: MutableMap<String, String> = HashMap()
            properties["keywords"] = keywords
            Analytics.trackEvent("Search", properties)

            val searchParameter = SearchParameter(
                true, keywords, before, after, includeComment, order
            )
            val data = Intent()
            data.putExtra("searchParameter", searchParameter)
            setResult(2, data)
            finish()
        } else {
            model.infoMsg.postValue(Event("Search keyword is empty!"))
        }
    }

    private fun setIncludeCommentView(includeComment: Boolean) {
        model.isIncludeComment.value = includeComment
        val colorAllButtonAlt = Utils.getColor(this, R.attr.color_all_button_alt)
        val colorAllCardBackground = Utils.getColor(this, R.attr.color_all_card_background)
        val colorAllFontAlt = Utils.getColor(this, R.attr.color_all_font_alt)
        val colorAllFont = Utils.getColor(this, R.attr.color_all_font)
        if (includeComment) {
//            model.infoMsg.postValue(Event("includeComment = true"))
            binding.includeLayout.setBackgroundColor(colorAllButtonAlt)
            binding.includeText0.setTextColor(colorAllFont)
            binding.includeText1.setTextColor(colorAllFont)
            binding.postOnlyLayout.setBackgroundColor(colorAllCardBackground)
            binding.postOnlyText0.setTextColor(colorAllFontAlt)
            binding.postOnlyText1.setTextColor(colorAllFontAlt)
        } else {
//            model.infoMsg.postValue(Event("includeComment = false"))
            binding.postOnlyLayout.setBackgroundColor(colorAllButtonAlt)
            binding.postOnlyText0.setTextColor(colorAllFont)
            binding.postOnlyText1.setTextColor(colorAllFont)
            binding.includeLayout.setBackgroundColor(colorAllCardBackground)
            binding.includeText0.setTextColor(colorAllFontAlt)
            binding.includeText1.setTextColor(colorAllFontAlt)
        }
    }

    private fun dateToUnixTimestamp(s: String): Long? {
        var res: Long? = -1
        try {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = simpleDateFormat.parse(s)
            res = date?.time?.div(1000)
        } catch (e: ParseException) {
        } finally {
            Log.d("SearchActivity", "date $s to timestamp $res")
            return res
        }
    }

    fun dateDialogFunc(view: View) {
        val startDate = binding.startDate
        val endDate = binding.endDate

        val ca = Calendar.getInstance()
        var mYear = ca[Calendar.YEAR]
        var mMonth = ca[Calendar.MONTH]
        var mDay = ca[Calendar.DAY_OF_MONTH]

        val datePickerDialog = DatePickerDialog(
            this,
            DatePickerDialog.THEME_HOLO_DARK,
            { _, year, month, dayOfMonth ->
                mYear = year
                mMonth = month
                mDay = dayOfMonth
                val strDate = "${year}-${month + 1}-${dayOfMonth}"
                when (view.id) {
                    R.id.start_date -> {
                        this@SearchActivity.model.startDateString.value = strDate
                        startDate.text = strDate
                    }
                    R.id.end_date -> {
                        this@SearchActivity.model.endDateString.value = strDate
                        endDate.text = strDate
                    }
                }
            },
            mYear, mMonth, mDay
        )
        datePickerDialog.show()
    }
}