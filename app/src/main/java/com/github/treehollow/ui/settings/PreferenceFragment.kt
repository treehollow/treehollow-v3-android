package com.github.treehollow.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingFragment
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.data.model.defaultBooleanPrefs
import com.github.treehollow.databinding.FragmentPreferencesBinding
import com.github.treehollow.repository.PreferencesRepository
import com.github.treehollow.utils.Const


class PreferenceFragment : DataBindingFragment() {

    val model: SettingsViewModel by activityViewModels()
    private lateinit var fragmentContext: SettingsActivity
    private val switchToString = mapOf(
        R.id.dark_theme_switch to "dark_mode",
        R.id.switch6 to "fold_hollows",
    )
    private lateinit var blockWordAdapter: BlockWordAdapter
    private var blockList = MutableList(0) { "" }

    private fun onCheckedChanged(switch: CompoundButton, isChecked: Boolean) {

        val name = switchToString[switch.id]
        if (name != null) {
            val oldValue = PreferencesRepository.getBooleanPreference(
                fragmentContext, name, defaultBooleanPrefs[name] ?: false
            )
            if (isChecked != oldValue) {
                Log.d("PreferenceFragment", "onCheckedChanged() call setBooleanPreference")
                PreferencesRepository.setBooleanPreference(fragmentContext, name, isChecked)
                if (name == "dark_mode") {
                    val builder = AlertDialog.Builder(fragmentContext)
                    builder.setTitle(R.string.successful_setting)
                    builder.setMessage(R.string.restart_message)
                    builder.setNeutralButton(R.string.ok) { _, _ ->
                        Toast.makeText(fragmentContext, R.string.ok, Toast.LENGTH_SHORT).show()
                    }
                    builder.show()
                } else {
                    model.userPrefs.value?.booleanPrefs?.put(name, isChecked)
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context as SettingsActivity

        val list = model.userPrefs.value!!.blockWords.split(",")
        for (s: String in list) {
            blockList.add(s)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentPreferencesBinding by binding(
            inflater,
            R.layout.fragment_preferences,
            container
        )
        val view = binding.root
        binding.apply {
            lifecycleOwner = this@PreferenceFragment
            vm = model
        }

        binding.darkThemeSwitch.setOnCheckedChangeListener { button: CompoundButton, isChecked: Boolean ->
            onCheckedChanged(button, isChecked)
        }
        binding.switch6.setOnCheckedChangeListener { button: CompoundButton, isChecked: Boolean ->
            onCheckedChanged(button, isChecked)
        }
        binding.loggingDevice.setOnClickListener {
            fragmentContext.replaceFragment(DevicesListFragment())
        }
        blockWordAdapter = BlockWordAdapter(activity as Context, blockList)
//        binding.blockWordsList.itemsCanFocus = true
//        binding.blockWordsList.adapter = blockWordAdapter

        val prefs = fragmentContext.getSharedPreferences(Const.PreferenceKey, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == "blockWords") {
//                    Log.d("prefs", "changed")
                blockWordAdapter.notifyDataSetChanged()
            }
        }

        val savedAnnouncement =
            TreeHollowApplication.Companion.Config.getConfigItemString("saved_announcement")
        if (savedAnnouncement != null && savedAnnouncement.isNotEmpty()) {
            binding.clearAnnouncement.setOnClickListener {
                TreeHollowApplication.Companion.Config.setConfigItemString(
                    "saved_announcement", ""
                )
                Toast.makeText(fragmentContext, "现在回到主界面并刷新会重新显示公告", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.clearAnnouncement.visibility = View.GONE
        }
        return view
    }
}

class BlockWordAdapter(
    private val context: Context,
    private var dataSource: MutableList<String>,
) : BaseAdapter() {

    private var mInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private fun updateBlockWordsPreference() {
        val str = dataSource.joinToString(separator = ",")
        PreferencesRepository.setStringPreference(context, "blockWords", str)
    }

    override fun getCount(): Int {
        return dataSource.size + 1
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return if (position < count - 1) {
            position.toLong()
        } else {
            -1
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if (position == count - 1) {
            if (convertView != null) {
                if (convertView.tag == "add button") {
                    return convertView
                }
            }
            val curView = mInflater.inflate(R.layout.blockword_add, parent, false)
            curView.tag = "add button"
            curView.setOnClickListener {
                dataSource.add("")
                updateBlockWordsPreference()
            }
            return curView
        } else {
            var curView = convertView ?: mInflater.inflate(R.layout.blockword_entry, parent, false)
            val holder: BlockEntryHolder
            if (convertView == null) {
                holder = BlockEntryHolder(curView)
                curView.tag = holder
            } else {
                if (curView.tag == "add button") {
                    curView = mInflater.inflate(R.layout.blockword_entry, parent, false)
                    holder = BlockEntryHolder(curView)
                    curView.tag = holder
                } else {
                    holder = curView.tag as BlockEntryHolder
                }
            }

            val curStr = dataSource.elementAt(position)
            holder.editText.setText(curStr)
            holder.editText.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    val textView = v as EditText
                    val s = textView.text.toString()
                    dataSource[position] = s
                    updateBlockWordsPreference()
                }
            }
            holder.editText.tag = "blockWordEntry"

            if (position == 0) {
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_cancel)
                holder.editText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
                holder.editText.onRightDrawableClicked {
                    it.text.clear()
                    dataSource[0] = ""
                    updateBlockWordsPreference()
                }
            } else {
                val imageResId = R.drawable.ic_delete_option
                val drawable = ContextCompat.getDrawable(context, imageResId)
                    ?: throw IllegalArgumentException("Cannot load drawable $imageResId")
                holder.editText.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
                holder.editText.onRightDrawableClicked {
                    dataSource.removeAt(position)
                    updateBlockWordsPreference()
                }
            }

            return curView
        }
    }
}

private class BlockEntryHolder(view: View) {
    val editText: EditText = view.findViewById(R.id.block_word_entry)
}

fun EditText.onRightDrawableClicked(onClicked: (view: EditText) -> Unit) {
    this.setOnTouchListener { v, event ->
        var hasConsumed = false
        if (v is EditText) {
            if (event.x >= v.width - v.totalPaddingRight) {
                if (event.action == MotionEvent.ACTION_UP) {
                    onClicked(this)
                }
                hasConsumed = true
            }
        }
        hasConsumed
    }
}
