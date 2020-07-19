package com.breakremindertimer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {

        //Users preference
        val themePreference = PreferenceManager.getDefaultSharedPreferences(this)
        var text: String? = themePreference.getString("color_theme", "")

        when (text) {
            "AppThemeDark" -> {
                setTheme(R.style.AppThemeDark)
            }
            "AppThemeBlackAndWhite" -> {
                setTheme(R.style.AppThemeBlackAndWhite)
            }
            else -> {
                setTheme(R.style.AppTheme)
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        themePreference.registerOnSharedPreferenceChangeListener(this)

    }

    //Set activity
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    //Back to main activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    //Listener change user preference
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "color_theme") {
            restartApp()
        }
    }

    //Unregister listener
    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    //Reset Application TODO
    fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.animation_activity_start,R.anim.animation_activity_end)
    }
}