package com.alesandro.webchecker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Clase de entrada a la aplicación
 *
 * @author Alesandro Quirós Gobbato
 */
class MainActivity : AppCompatActivity() {
    private val workTag = "WebCheckerWork"
    private var workId = UUID.randomUUID()
    private var semaforo = "R"
    private lateinit var lista: RecyclerView
    private lateinit var db: ResultadoDatabaseHelper
    private lateinit var resultadoList: MutableList<Resultado>
    private lateinit var resultadoAdapter: ResultadoAdapter
    private lateinit var tvStatus: TextView

    /**
     * Función que se ejecuta al iniciar la aplicación
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        db = ResultadoDatabaseHelper(this)
        tvStatus = findViewById(R.id.tvStatus)
        if (semaforo == "V") {
            tvStatus.text = "On"
        } else {
            tvStatus.text = "Off"
        }
        lista = findViewById(R.id.lista)
        resultadoList = db.getAllResultados()
        lista.layoutManager = LinearLayoutManager(this)
        resultadoAdapter = ResultadoAdapter(resultadoList) { resultado ->
            deleteResultado(resultado)
        }
        lista.adapter = resultadoAdapter
        lista.itemAnimator = DefaultItemAnimator()
        val urlField = findViewById<EditText>(R.id.etUrl)
        val palabraField = findViewById<EditText>(R.id.etWord)
        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val btnStop = findViewById<Button>(R.id.btnOff)
        btnPlay.setOnClickListener {
            this.semaforo = "V"
            tvStatus.text = "On"
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
            sharedPreferences.edit().putString("semaforo", semaforo).apply()
            WorkManager.getInstance(this).cancelAllWorkByTag(this.workTag)
            val workRequest = PeriodicWorkRequestBuilder<WebCheckerWorker>(
                15, // Intervalo mínimo de 15 minutos
                TimeUnit.MINUTES
            ).addTag(this.workTag).build()

            // Encolar el trabajo periódico
            WorkManager.getInstance(this).enqueue(workRequest)

            // Guardar el ID del trabajo en ejecución
            this.workId = workRequest.id

            val workManager = WorkManager.getInstance(this)
            workManager.getWorkInfoByIdLiveData(workRequest.id).observe(this) { workInfo ->
                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                    // Update the list with new results from the database
                    resultadoList.clear()
                    resultadoList.addAll(db.getAllResultados())
                    resultadoAdapter.notifyDataSetChanged()
                }
            }
        }// Listener para el botón "Stop"
        btnStop.setOnClickListener {
            println("Pulso boton stop")
            this.semaforo="R"
            tvStatus.text = "Off"
            val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
            sharedPreferences.edit().putString("semaforo", semaforo).apply()
            stopWork()
        }
        // Listener para capturar cambios en el campo de texto de la URL
        urlField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se realiza ninguna acción antes del cambio de texto
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Se puede manejar el texto mientras cambia (opcional)
            }

            override fun afterTextChanged(s: Editable?) {
                val url = s.toString()
                val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
                sharedPreferences.edit().putString("url", url).apply()
                println("URL ingresada: $url")
            }
        })
        // Listener para capturar cambios en el campo de texto de la palabra clave
        palabraField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No se realiza ninguna acción antes del cambio de texto
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Se puede manejar el texto mientras cambia (opcional)
            }

            override fun afterTextChanged(s: Editable?) {
                val word = s.toString()
                val sharedPreferences = getSharedPreferences("WebCheckerPrefs", MODE_PRIVATE)
                sharedPreferences.edit().putString("word", word).apply()
                println("Palabra ingresada: $word")
            }
        })
    }

    /**
     * Función para detener la tarea de fondo y cerrar la aplicación
     */
    private fun stopWork() {
        // Cancelar trabajos asociados con la etiqueta
        //WorkManager.getInstance(this).cancelAllWorkByTag(this.workTag)

        // Cancelar trabajos específicos por ID
        WorkManager.getInstance(this).cancelWorkById(this.workId)
        //WorkManager.getInstance(this).cancelAllWork()
        WorkManager.getInstance(this).pruneWork()
        WorkManager.getInstance(this).getWorkInfosByTag(workTag).get().forEach { workInfo ->
            println("Trabajo ID: ${workInfo.id}, Estado: ${workInfo.state}")

            // Cancelar solo si el trabajo está encolado o en ejecución
            if (workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING) {
                WorkManager.getInstance(this).cancelWorkById(workInfo.id)
                println("Trabajo con ID ${workInfo.id} cancelado")
            }
        }
    }

    /**
     * Función que elimina un resultado de la lista
     *
     * @param resultado resultado a eliminar
     */
    private fun deleteResultado(resultado: Resultado) {
        val rowsDeleted = db.deleteResultadoById(resultado.id)
        if (rowsDeleted > 0) {
            resultadoAdapter.removeResultado(resultado)
            Toast.makeText(this, "Resultado eliminado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No se ha podido eliminar ese resultado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Variable para la detección de actualizaciones de WebCheckerWorker
     */
    private val resultUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Update RecyclerView when a new result is added
            resultadoList.clear()
            resultadoList.addAll(db.getAllResultados())
            resultadoAdapter.notifyDataSetChanged()
        }
    }

    /**
     * Función que inicia la detección de actualizaciones de WebCheckerWorker
     */
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.alesandro.webchecker.RESULT_UPDATED")
        LocalBroadcastManager.getInstance(this).registerReceiver(resultUpdateReceiver, filter)
    }

    /**
     * Función que para la detección de actualizaciones de WebCheckerWorker
     */
    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resultUpdateReceiver)
    }
}