package com.javis.launcher.ui.onboarding

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: SharedPreferences
) : ViewModel() {

    fun saveProfile(name: String, nickname: String) {
        prefs.edit()
            .putString("user_name", name.trim())
            .putString("user_nickname", nickname.trim().ifBlank { "Sir" })
            .apply()
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
    }
}
