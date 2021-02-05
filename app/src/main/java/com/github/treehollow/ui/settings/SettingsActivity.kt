package com.github.treehollow.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.github.treehollow.R
import com.github.treehollow.base.DataBindingActivity
import com.github.treehollow.databinding.ActivitySettingsBinding
import com.github.treehollow.push.WebSocketService
import com.github.treehollow.repository.PreferencesRepository
import com.github.treehollow.ui.MainActivity
import com.github.treehollow.utils.Utils.setActivityTheme

class SettingsActivity : DataBindingActivity(), FragmentChangeListener {
    @VisibleForTesting

    private val binding: ActivitySettingsBinding by binding(R.layout.activity_settings)
    val model: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setActivityTheme(this)
        super.onCreate(savedInstanceState)

        binding.apply {
            lifecycleOwner = this@SettingsActivity
        }
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<SettingsFragment>(R.id.fragment_container_view, "settings_main")
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        model.getPushInfo()
        model.gonnaQuit.observe(this, { t ->
            if (t) {
                stopService(Intent(this, WebSocketService::class.java))
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        )
        model.userPrefs.value = PreferencesRepository.getPreferences(this)
    }


    override fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container_view, fragment)
            setReorderingAllowed(true)
            addToBackStack("settings_preference")
        }
    }

}

interface FragmentChangeListener {
    fun replaceFragment(fragment: Fragment)
}