package com.checklist.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "checklist_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val HAS_PURCHASED_KEY = booleanPreferencesKey("has_purchased")
    }
    
    val hasPurchased: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_PURCHASED_KEY] ?: false
        }
    
    suspend fun setHasPurchased(hasPurchased: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_PURCHASED_KEY] = hasPurchased
        }
    }
}