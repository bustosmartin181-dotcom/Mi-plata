package com.bustos.miplata.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "mi_plata_nativa")

/**
 * Stores the original Mi Plata backup as an opaque JSON document. Unknown fields are
 * deliberately retained so importing a backup never discards data from newer versions.
 */
class BackupStore(private val context: Context) {
    private val backupKey = stringPreferencesKey("miplata_original_json")

    val backupJson: Flow<String?> = context.dataStore.data.map { it[backupKey] }

    suspend fun importOriginalBackup(json: String) {
        validateMiPlataBackup(json)
        context.dataStore.edit { it[backupKey] = json }
    }

    suspend fun saveEditedBackup(json: String) {
        validateMiPlataBackup(json)
        context.dataStore.edit { it[backupKey] = json }
    }

    fun validateMiPlataBackup(json: String): JSONObject {
        val root = JSONObject(json)
        require(root.opt("items") is org.json.JSONArray) { "Falta la lista de gastos importantes." }
        require(root.opt("spent") is org.json.JSONArray) { "Falta la lista de gastos anotados." }
        require(root.opt("ingresos") == null || root.opt("ingresos") is org.json.JSONArray) {
            "La lista de ingresos tiene un formato inesperado."
        }
        return root
    }
}
