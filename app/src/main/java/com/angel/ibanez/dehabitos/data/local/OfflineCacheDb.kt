package com.angel.ibanez.dehabitos.data.local

import android.content.ContentValues
import android.content.Context
import net.sqlcipher.Cursor
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper

class OfflineCacheDb(
    context: Context,
    private val passphrase: String
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        SQLiteDatabase.loadLibs(context)
    }

    companion object {
        const val DATABASE_NAME = "habitos_cache_v2.db"
        const val DATABASE_VERSION = 1
        const val TABLE_CACHE = "cache"
        const val COL_COLECCION = "coleccion"
        const val COL_DOC_ID = "doc_id"
        const val COL_NINO_ID = "nino_id"
        const val COL_DATA = "data"
        const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CACHE (
                $COL_COLECCION TEXT NOT NULL,
                $COL_DOC_ID TEXT NOT NULL,
                $COL_NINO_ID TEXT,
                $COL_DATA TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                PRIMARY KEY ($COL_COLECCION, $COL_DOC_ID)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHE")
        onCreate(db)
    }

    fun guardar(coleccion: String, docId: String, ninoId: String?, data: String) {
        val db = getWritableDatabase(passphrase)
        val values = ContentValues().apply {
            put(COL_COLECCION, coleccion)
            put(COL_DOC_ID, docId)
            put(COL_NINO_ID, ninoId)
            put(COL_DATA, data)
            put(COL_TIMESTAMP, System.currentTimeMillis())
        }
        db.replace(TABLE_CACHE, null, values)
    }

    fun obtener(coleccion: String, docId: String): String? {
        val db = getReadableDatabase(passphrase)
        val cursor: Cursor = db.query(
            TABLE_CACHE,
            arrayOf(COL_DATA),
            "$COL_COLECCION = ? AND $COL_DOC_ID = ?",
            arrayOf(coleccion, docId),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    fun obtenerPorNino(coleccion: String, ninoId: String): List<String> {
        val db = getReadableDatabase(passphrase)
        val cursor: Cursor = db.query(
            TABLE_CACHE,
            arrayOf(COL_DATA),
            "$COL_COLECCION = ? AND $COL_NINO_ID = ?",
            arrayOf(coleccion, ninoId),
            null, null, "$COL_TIMESTAMP DESC"
        )
        return cursor.use {
            val result = mutableListOf<String>()
            while (it.moveToNext()) result.add(it.getString(0))
            result
        }
    }

    fun eliminarPorNino(coleccion: String, ninoId: String) {
        getWritableDatabase(passphrase).delete(
            TABLE_CACHE,
            "$COL_COLECCION = ? AND $COL_NINO_ID = ?",
            arrayOf(coleccion, ninoId)
        )
    }
}
