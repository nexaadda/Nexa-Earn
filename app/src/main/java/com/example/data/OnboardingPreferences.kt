package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nexaEarnPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nexaearn_user_prefs"
)

class OnboardingPreferences(private val context: Context) {

    private fun onboardingKey(userId: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("onboarding_completed_$userId")

    fun observeOnboardingCompleted(userId: String): Flow<Boolean> {
        val key = onboardingKey(userId)
        return context.nexaEarnPreferencesDataStore.data.map { prefs ->
            prefs[key] ?: false
        }
    }

    suspend fun setOnboardingCompleted(userId: String, completed: Boolean) {
        val key = onboardingKey(userId)
        context.nexaEarnPreferencesDataStore.edit { prefs ->
            prefs[key] = completed
        }
    }
}
