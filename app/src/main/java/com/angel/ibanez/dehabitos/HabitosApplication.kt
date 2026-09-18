package com.angel.ibanez.dehabitos

import android.app.Application
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.NotificacionesUtils
import com.angel.ibanez.dehabitos.workers.ProgramadorTrabajos
import com.google.firebase.FirebaseApp
import com.mikepenz.iconics.Iconics

class HabitosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase explícitamente
        FirebaseApp.initializeApp(this)
        
        // Inicializar configuración global del dispositivo
        DispositivoConfig.inicializar(this)

        NotificacionesUtils.crearCanales(this)
        ProgramadorTrabajos.programar(this)
    }
}
