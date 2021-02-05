package com.github.treehollow.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.text.*
import android.text.method.ArrowKeyMovementMethod
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import com.github.treehollow.R
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.LinkRedirect
import com.github.treehollow.data.PostRedirect
import com.github.treehollow.data.Redirect
import com.github.treehollow.repository.PreferencesRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.*


object Utils {

    fun getColor(context: Context, resId: Int): Int {
        val typedValue = TypedValue()
        val theme: Theme = context.theme
        theme.resolveAttribute(resId, typedValue, true)
        return typedValue.data
    }

    /**
     * Supported algorithms on Android:
     *
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
    fun hashString(type: String = "SHA-256", input: String): String {
        val hexChars = "0123456789abcdef"
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }

        return result.toString()
    }

    enum class BottomStatus {
        REFRESHING, NO_MORE, NETWORK_ERROR, IDLE
    }

    interface HtmlAnchorClickListener {
        fun onHyperLinkClicked(name: String)
    }

    class HtmlAnchorClickListenerImpl(val context: Activity) : HtmlAnchorClickListener {
        override fun onHyperLinkClicked(name: String) {
            context.launchCustomTab(Uri.parse(name))
        }
    }

    fun addClickableSpan(
        linkableTextView: TextView?,
        htmlString: String,
        listener: HtmlAnchorClickListener
    ) {
        linkableTextView?.let {
            val sequence = HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
            val spannableString = SpannableStringBuilder(sequence)
            val urls = spannableString.getSpans(0, sequence.length, URLSpan::class.java)
            urls.forEach { span ->
                with(spannableString) {
                    val start = getSpanStart(span)
                    val end = getSpanEnd(span)
                    val flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    val linkColor =
                        getColor(
                            it.context,
                            R.attr.color_all_font
                        ) //TODO: Is directly referring this color a must?

                    val clickable = object : ClickableSpan() {

                        override fun onClick(view: View) {
                            // Prevent CheckBox state from being toggled when link is clicked
                            linkableTextView.cancelPendingInputEvents()
                            removeRippleEffectFromCheckBox(linkableTextView)
                            listener.onHyperLinkClicked(span.url)
                        }

                        override fun updateDrawState(textPaint: TextPaint) {
                            textPaint.color = linkColor
                            textPaint.isUnderlineText = true
                        }
                    }
                    setSpan(clickable, start, end, flags)
                    setSpan(ForegroundColorSpan(linkColor), start, end, flags)
                    removeSpan(span)
                }

                with(it) {
                    text = spannableString
                    linksClickable = true
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
        }
    }

    fun removeRippleEffectFromCheckBox(textView: TextView) {
        var drawable = textView.background
        if (drawable is RippleDrawable) {
            drawable = drawable.findDrawableByLayerId(0)
            textView.background = drawable
        }
    }

    @ExperimentalTime
    fun displayTimestamp(timestamp: Long): String = Calendar.getInstance().let {
        it.time = Date(timestamp)
        (System.currentTimeMillis() - it.timeInMillis).milliseconds.run {
            when {
                this < 1.minutes -> "刚刚"
                this < 1.hours -> "${inMinutes.toInt()}分钟前"
                this < 1.days -> "${inHours.toInt()}小时前"
                this < 2.days -> "昨天"
                this < 30.days -> "${inDays.toInt()}天前"
                this < 6 * 4 * 7.days -> SimpleDateFormat(
                    "MM月dd日",
                    Locale.getDefault()
                ).format(it.time)
                else -> SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(it.time)
            }
        }
    }

//    fun SpannableString.links(activity: Activity): List<Pair<String, () -> Unit>> {
//        val ret = mutableListOf<Pair<String, () -> Unit>>()
//        Regex(Patterns.WEB_URL.pattern()).findAll(this).forEach { raw ->
//            Regex("[\u0000-\u007F]+").findAll(raw.value)
//                .filter { Regex(Patterns.WEB_URL.pattern()).matches(it.value) }
//                .forEach {
//                    val url =
//                        if (it.value.matches(Regex("^https?://.*"))) it.value
//                        else "http://${it.value}"
//                    setSpan(
//                        object : ClickableSpan() {
//                            override fun onClick(widget: View) {
//                                activity.launchCustomTab(Uri.parse(url))
//                            }
//                        },
//                        raw.range.first + it.range.first, raw.range.first + it.range.last + 1,
//                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                    ret += "访问 " + it.value
//                        .replace(Regex("^https?://"), "")
//                        .replace(Regex("^www\\."), "")
//                        .run {
//                            if (length >= 27) substring(0, 25) + '\u22EF' else this
//                        } to { activity.launchCustomTab(Uri.parse(url)) }
//                }
//        }
////        Regex("wkfg://[0-9]+").findAll(this).forEach {
////            val action = {
////                fun View.pair() = android.util.Pair.create(this, this.transitionName)
////                val options = ActivityOptions.makeSceneTransitionAnimation(
////                    activity,
////                    when (activity) {
////                        is MainActivity -> activity.binding.fab
////                        is PostDetailActivity -> activity.binding.bottomBar
////                        else -> error("")
////                    }.pair(),
////                    when (activity) {
////                        is MainActivity -> activity.binding.appbar
////                        is PostDetailActivity -> activity.binding.appbar
////                        else -> error("")
////                    }.pair(),
////                    activity.findViewById<View>(android.R.id.statusBarBackground).pair(),
////                )
////                activity.window.exitTransition = Slide(Gravity.START).apply {
////                    interpolator = AccelerateInterpolator()
////                }
////                activity.window.reenterTransition = Slide(Gravity.START).apply {
////                    interpolator = DecelerateInterpolator()
////                }
////                activity.startActivity(
////                    Intent(Intent.ACTION_VIEW, Uri.parse(it.value)).apply {
////                        setClass(activity, PostDetailActivity::class.java)
////                    }, options.toBundle()
////                )
////            }
////            setSpan(
////                object : ClickableSpan() {
////                    override fun onClick(widget: View) {
////                        action()
////                    }
////                }, it.range.first, it.range.last + 1,
////                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
////            )
////            ret += "跳转到 " + it.value to action
////        }
//        Regex(Patterns.EMAIL_ADDRESS.pattern()).findAll(this).forEach {
//            getSpans(
//                it.range.first, it.range.last + 1, ClickableSpan::class.java
//            ).forEach(this::removeSpan)
//            setSpan(
//                object : ClickableSpan() {
//                    override fun onClick(widget: View) {
//                        activity.startActivity(
//                            Intent(Intent.ACTION_VIEW, Uri.parse("mailto:${it.value}"))
//                        )
//                    }
//                }, it.range.first, it.range.last + 1,
//                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//            ret += "发送邮件到 " + it.value to {
//                activity.startActivity(
//                    Intent(Intent.ACTION_VIEW, Uri.parse("mailto:${it.value}"))
//                )
//            }
//        }
//        return ret
//    }

    fun Context.launchCustomTab(uri: Uri) {
        CustomTabsIntent.Builder().apply {
            setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().run {
                setToolbarColor(TypedValue().apply {
                    theme.resolveAttribute(android.R.attr.statusBarColor, this, true)
                }.data)
                build()
            })
            setShareState(CustomTabsIntent.SHARE_STATE_ON)
        }.build().launchUrl(this, uri)
    }


    fun copy(context: Context, s: String, view: View) {
        val myClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val myClip: ClipData = ClipData.newPlainText("note_copy", s)
        myClipboard.setPrimaryClip(myClip)

        Snackbar.make(view, "已复制: $s", Snackbar.LENGTH_SHORT).apply {
//            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 1
            show()
        }
    }

    fun showSelectDialog(context: Context, spannable: SpannableString) {
        MaterialAlertDialogBuilder(context).run {
            setMessage(spannable)
            setCancelable(true)
            create()
        }.apply {
            show()
            window?.apply {
                decorView.findViewById<TextView>(android.R.id.message)?.apply {
                    setTextIsSelectable(true)
                    movementMethod = MagicSelectableClickableMovementMethod
                    textSize = 18F
                }
            }
        }
    }

    object MagicSelectableClickableMovementMethod : ArrowKeyMovementMethod() {
        override fun onTouchEvent(
            widget: TextView?, buffer: Spannable?, event: MotionEvent?
        ): Boolean {
            if (event!!.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_DOWN) {
                val tx = event.x.toInt() - widget!!.totalPaddingLeft + widget.scrollX
                val ty = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                val line = widget.layout.getLineForVertical(ty)
                val offset = widget.layout.getOffsetForHorizontal(line, tx.toFloat())
                val spans = buffer!!.getSpans<ClickableSpan>(offset, offset)
                return spans.isNotEmpty().apply {
                    if (this && event.action == MotionEvent.ACTION_UP)
                        spans.forEach { it.onClick(widget) }
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }
    }

    data class ImageScale(
        val w: Int,
        val h: Int
    )

    // Image Processing
    fun calculateCompressScale(imgWidth: Int, imgHeight: Int): ImageScale {
        val screenWidth = 1080
        val screenHeight = 1920
        var scale = 1.0F
        val scaleWidth: Float = imgWidth.toFloat() / screenWidth.toFloat()
        val scaleHeight: Float = imgHeight.toFloat() / screenHeight.toFloat()
        if (scaleWidth > scaleHeight && scaleWidth > 1) {
            scale = scaleWidth
        } else if (scaleWidth < scaleHeight && scaleHeight > 1) {
            scale = scaleHeight
        }
        return ImageScale(
            (imgWidth.toFloat() / scale).toInt(),
            ((imgHeight.toFloat() / scale).toInt())
        )
    }

    fun compressDisplayImage(image: Bitmap?): Bitmap? {
        // down sample the origin image for display
        if (image != null) {
            val imgWidth = image.width
            val imgHeight = image.height
            val screenWidth = 1080
            val screenHeight = 1920
            var scale = 1.0F
            val scaleWidth: Float = imgWidth.toFloat() / screenWidth.toFloat()
            val scaleHeight: Float = imgHeight.toFloat() / screenHeight.toFloat()
            if (scaleWidth > scaleHeight && scaleWidth > 1) {
                scale = scaleWidth
            } else if (scaleWidth < scaleHeight && scaleHeight > 1) {
                scale = scaleHeight
            }
            val matrix = Matrix()
            matrix.postScale(1.0F / scale, 1.0F / scale)
            return Bitmap.createBitmap(
                image, 0, 0, imgWidth,
                imgHeight, matrix, true
            )
        }
        return null
    }

    fun setActivityTheme(context: Context) {
        val userPrefs = PreferencesRepository.getPreferences(context)
        val useDarkMode = userPrefs.booleanPrefs["dark_mode"]
        if (useDarkMode == true) {
            context.setTheme(R.style.Theme_TreeHollow_dark)
        } else {
            val tmp = TreeHollowApplication.Companion.Config.getConfigItemString("config_url")
            when (tmp) {
                Const.ConfigURLs.thu_hollow_config_url ->
                    context.setTheme(R.style.Theme_TreeHollow_light_thu)
                Const.ConfigURLs.pku_hollow_config_url ->
                    context.setTheme(R.style.Theme_TreeHollow_light_pku)
                else ->
                    context.setTheme(R.style.Theme_TreeHollow_light_other)
            }
        }
    }

    fun urlsFromText(text: String): List<Redirect> {
        val rtn: MutableList<Redirect> = mutableListOf()
        "(?![^.@a-zA-Z0-9_])((?:https?://)?(?:(?:[\\w-]+\\.)+[a-zA-Z]{2,3}|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::\\d{1,5})?(?:/[\\w~!@#\$%^&*()\\-_=+\\[\\]{};:,./?|]*)?)(?![a-zA-Z0-9])".toRegex()
            .findAll(text)
            .forEach { rtn += LinkRedirect(it.value) }
        "#\\d{1,7}".toRegex().findAll(text)
            .forEach {
                val pid = it.value.substring(1).toInt()
                rtn += PostRedirect(pid, -1)
            }
        return rtn
    }

    fun trimString(s: String, maxChar: Int): String {
        if (s.length <= maxChar)
            return s
        return s.substring(0, maxChar - 3) + "..."
    }

    fun onlyRunOnce(name: String, task: () -> Unit) {
        val s = TreeHollowApplication.Companion.Config.getConfigItemString("did_$name")
        if (s == null) {
            TreeHollowApplication.Companion.Config.setConfigItemString("did_$name", "done")
            task()
        }
    }
}