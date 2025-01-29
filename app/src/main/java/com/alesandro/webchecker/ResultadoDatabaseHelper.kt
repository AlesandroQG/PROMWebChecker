package com.alesandro.webchecker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Clase con funciones para interactuar con la base de datos de SQLite
 */
class ResultadoDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "resultados.db";
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "resultados"
        // Columnas de la tabla
        private const val COLUMN_ID = "id"
        private const val COLUMN_URL = "url"
        private const val COLUMN_RESULTADO = "resultado"
    }

    /**
     * Función que crea la tabla
     *
     * @param db base de datos
     */
    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery =
            "CREATE TABLE $TABLE_NAME (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY, " +
                    "$COLUMN_URL TEXT, " +
                    "$COLUMN_RESULTADO TEXT"
        db?.execSQL(createTableQuery)
    }

    /**
     * Función que modífica la base de datos
     *
     * @param db base de datos
     * @param oldVersion versión anterior
     * @param newVersion versión nueva
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropTableQuery =
            "DROP TABLE IF EXISTS $TABLE_NAME"
        db?.execSQL(dropTableQuery)
        onCreate(db)
    }

    /**
     * Pasado un objeto resultado, la añade a la base de datos
     *
     * @param resultado resultado a añadir
     */
    fun insertResultado(resultado: Resultado) {
        //la abro en modo escritura
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(COLUMN_URL, resultado.url)
                put(COLUMN_RESULTADO, resultado.resultado)
                //en este caso ID es autonumérico
            }
            //insertamos datos
            db.insert(TABLE_NAME, null, values)
            //y ahora a cerrar
            //db.close()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Lee la base de datos y rellena una List de tipo Resultado
     *
     * @return Una lista de tipo Resultado
     */
    fun getAllResultados(): MutableList<Resultado> {
        //creo una lista mutable para poder cambiar cosas
        val resultadoList = mutableListOf<Resultado>()
        //la abro en modo lectura
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        //lanza un cursor
        val cursor = db.rawQuery(query, null)
        //itera mientras que exista otro
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL))
            val resultado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESULTADO))
            //creamos un objeto temporal de tipo Task
            val rtdo = Resultado(id, url, resultado)
            //añadimos la task
            resultadoList.add(rtdo)
        }
        //cerrar las conexiones
        cursor.close()
        db.close()
        return resultadoList
    }

    /**
     * Elimina un punto por su id
     *
     * @param id id del punto
     * @return número de filas eliminadas
     */
    fun deleteResultadoById(id: Int): Int {
        val db = writableDatabase
        val whereClauses = "$COLUMN_ID = ?"
        val whereArgs = arrayOf(id.toString())
        //eliminar el objeto
        val rowsDeleted = db.delete(TABLE_NAME, whereClauses, whereArgs)
        db.close()
        return rowsDeleted
    }
}