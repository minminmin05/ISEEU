package com.iseeu.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local device state: which family this device belongs to, and the sharing toggle.
 * Read synchronously-ish (first()) by BootCompletedReceiver, which has no UI/ViewModel to hang off of.
 */
@Singleton
class PrefsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val familyCode: Flow<String?> = dataStore.data.map { it[FAMILY_CODE] }
    val selfUid: Flow<String?> = dataStore.data.map { it[SELF_UID] }
    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { it[ONBOARDED] ?: false }
    val isSharingEnabled: Flow<Boolean> = dataStore.data.map { it[SHARING_ENABLED] ?: true }

    suspend fun saveOnboarding(familyCode: String, uid: String) {
        dataStore.edit { prefs ->
            prefs[FAMILY_CODE] = familyCode
            prefs[SELF_UID] = uid
            prefs[ONBOARDED] = true
            prefs[SHARING_ENABLED] = true
        }
    }

    suspend fun setSharingEnabled(enabled: Boolean) {
        dataStore.edit { it[SHARING_ENABLED] = enabled }
    }

    companion object {
        private val FAMILY_CODE = stringPreferencesKey("family_code")
        private val SELF_UID = stringPreferencesKey("self_uid")
        private val ONBOARDED = booleanPreferencesKey("has_completed_onboarding")
        private val SHARING_ENABLED = booleanPreferencesKey("is_sharing_enabled")
    }
}
