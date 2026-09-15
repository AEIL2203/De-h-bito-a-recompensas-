package com.angel.ibanez.dehabitos.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.angel.ibanez.dehabitos.MainActivity
import com.angel.ibanez.dehabitos.databinding.ActivityBlockedBinding

class BlockedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nombreApp = intent.getStringExtra(EXTRA_NOMBRE_APP).orEmpty()
        binding.tvTitulo.text = "¡Se acabó el tiempo!"
        binding.tvMensaje.text = if (nombreApp.isBlank()) {
            "Esta app volvió a bloquearse. Canjea otra recompensa para seguir usándola."
        } else {
            "Se terminó el tiempo disponible para $nombreApp. Canjea otra recompensa para volver a usarla."
        }

        binding.btnVolver.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            finish()
        }

        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_NOMBRE_APP = "nombre_app"
    }
}
