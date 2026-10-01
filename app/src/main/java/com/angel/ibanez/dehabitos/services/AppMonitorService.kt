package com.angel.ibanez.dehabitos.services

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Build
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.angel.ibanez.dehabitos.models.Nino
import com.angel.ibanez.dehabitos.models.Recompensa
import com.angel.ibanez.dehabitos.models.SesionApp
import com.angel.ibanez.dehabitos.repository.EstadisticasRepository
import java.util.UUID
import com.angel.ibanez.dehabitos.repository.RecompensaRepository
import com.angel.ibanez.dehabitos.utils.AppsUtils
import com.angel.ibanez.dehabitos.utils.ConsumoRecompensas
import com.angel.ibanez.dehabitos.utils.DispositivoConfig
import com.angel.ibanez.dehabitos.utils.HorarioSuenoUtils
import com.angel.ibanez.dehabitos.utils.NotificacionesUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppMonitorService : AccessibilityService() {

    private val repository = RecompensaRepository()
    private val estadisticasRepository = EstadisticasRepository()
    private val usageStatsManager by lazy { getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listenerApps: ListenerRegistration? = null
    private var listenerRecompensas: ListenerRegistration? = null
    private var listenerNino: ListenerRegistration? = null
    private val appsPermitidas = mutableSetOf<String>()
    private val datosApps = mutableMapOf<String, Pair<String, String>>()
    private var sesionActiva: SesionApp? = null
    private var recompensas = emptyList<Recompensa>()
    private val consumosPendientes = mutableMapOf<String, Long>()
    private val consumosConfirmados = mutableMapOf<String, Long>()
    private var ninoIdEscuchando = ""
    private var paqueteEnPrimerPlano: String? = null
    private var horarioSueno: com.angel.ibanez.dehabitos.models.HorarioSueno? = null
    private var ultimoTick = 0L
    private var monitorJob: Job? = null
    private var sincronizando = false
    private var paqueteNotificado5Min: String? = null

    private val umbral5Min = 5 * 60 * 1000L
    private val idNotificacion5Min = 1001

    private var launcherPackageName: String? = null
    private val essentialPackages = setOf(
        "android",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.contacts",
        "com.google.android.packageinstaller",
        "com.android.systemui",
        "com.google.android.gsf",
        "com.google.android.gms",
        "com.google.android.inputmethod.latin"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        launcherPackageName = getLauncherPackageName()
        val ninoId = DispositivoConfig.obtenerNinoId()
        val modo = DispositivoConfig.obtenerModo()
        Log.d("AppMonitor", "onServiceConnected ninoId=$ninoId modo=$modo")
        if (ninoId.isNotBlank() && modo == DispositivoConfig.ModoDispositivo.HIJO) {
            Toast.makeText(this, "Control de apps activo", Toast.LENGTH_SHORT).show()
            iniciarEscuchas(ninoId)
            iniciarMonitor()
        }
    }

    private fun getLauncherPackageName(): String? {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun iniciarEscuchas(ninoId: String) {
        if (ninoId == ninoIdEscuchando) return
        scope.launch { sincronizarConsumoSeguro() }
        ninoIdEscuchando = ninoId
        listenerApps?.remove()
        listenerRecompensas?.remove()
        listenerApps = repository.escucharAppsPermitidasPorNino(
            ninoId,
            onUpdate = { lista ->
                appsPermitidas.clear()
                datosApps.clear()
                appsPermitidas.addAll(lista.map { it.packageName })
                lista.forEach { datosApps[it.packageName] = it.nombre to it.tutorId }
                Log.d("AppMonitor", "Apps controladas actualizadas: ${lista.size}")
            },
            onError = { Log.e("AppMonitor", "No se pudieron cargar las apps controladas", it) }
        )
        listenerRecompensas = repository.escucharRecompensasPorNino(
            ninoId,
            onUpdate = { lista ->
                lista.forEach { recompensa ->
                    consumosConfirmados[recompensa.id] = maxOf(
                        consumosConfirmados[recompensa.id] ?: 0,
                        recompensa.milisegundosConsumidos
                    )
                }
                recompensas = lista
                Log.d("AppMonitor", "Recompensas actualizadas: ${lista.size}")
            },
            onError = { Log.e("AppMonitor", "No se pudieron cargar las recompensas", it) }
        )
        listenerNino = FirebaseFirestore.getInstance().collection("ninos").document(ninoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                horarioSueno = snapshot.toObject(Nino::class.java)?.horarioSueno
            }
    }

    private fun iniciarMonitor() {
        if (monitorJob != null) return
        ultimoTick = SystemClock.elapsedRealtime()
        monitorJob = scope.launch {
            var desdeSincronizacion = 0L
            while (isActive) {
                delay(INTERVALO_MONITOR_MS)
                val ninoId = DispositivoConfig.obtenerNinoId()
                if (DispositivoConfig.obtenerModo() == DispositivoConfig.ModoDispositivo.HIJO && ninoId.isNotBlank()) {
                    obtenerPaqueteEnPrimerPlano()?.let { procesarPaquete(ninoId, it) }
                }
                val transcurrido = actualizarConsumoHastaAhora()
                val paquete = paqueteEnPrimerPlano
                if (paquete != null && appsPermitidas.contains(paquete)) {
                    desdeSincronizacion += transcurrido
                    val esHorarioSueno = HorarioSuenoUtils.estaEnHorarioSueno(horarioSueno)
                    val msDisponibles = tiempoDisponible(paquete)
                    if (esHorarioSueno) {
                        finalizarSesionActiva()
                        paqueteEnPrimerPlano = null
                        mostrarMensajeHorarioSueno(paquete)
                        sincronizarConsumoSeguro()
                    } else {
                        if (msDisponibles > umbral5Min) paqueteNotificado5Min = null
                        if (msDisponibles in 1..umbral5Min && paquete != paqueteNotificado5Min) {
                            notificarPocoTiempo(paquete, msDisponibles)
                            paqueteNotificado5Min = paquete
                        }
                        if (!tieneTiempo(paquete)) {
                            finalizarSesionActiva()
                            paqueteEnPrimerPlano = null
                            mostrarBloqueo(paquete)
                            sincronizarConsumoSeguro()
                        } else if (desdeSincronizacion >= INTERVALO_SINCRONIZACION_MS) {
                            sincronizarConsumoSeguro()
                            desdeSincronizacion = 0
                        }
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val modo = DispositivoConfig.obtenerModo()
        if (modo != DispositivoConfig.ModoDispositivo.HIJO) {
            return
        }
        val ninoId = DispositivoConfig.obtenerNinoId()
        if (ninoId.isBlank()) {
            return
        }
        iniciarEscuchas(ninoId)
        iniciarMonitor()

        event.packageName?.toString()?.let { procesarPaquete(ninoId, it) }
    }

    private fun esEsencial(packageName: String): Boolean {
        // Nuestra propia app siempre es esencial para que no se bloquee a sí misma
        if (packageName == this.packageName) return true

        // El launcher es esencial para poder navegar y entrar a nuestra app
        if (packageName == launcherPackageName) return true

        // Paquetes críticos del sistema que definimos manualmente
        if (essentialPackages.contains(packageName)) return true

        // YouTube, TikTok y Facebook NUNCA son esenciales
        val appsEntretenimiento = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.kids",
            "com.zhiliaoapp.musically", // TikTok
            "com.facebook.katana",
            "com.instagram.android",
            "com.netflix.mediaclient"
        )
        if (appsEntretenimiento.contains(packageName)) return false

        // Si es una app que el tutor ya marcó como controlada, NO es esencial (queremos controlarla)
        if (appsPermitidas.contains(packageName)) return false

        // Por último, si no tiene intención de lanzamiento (es un componente de fondo), lo consideramos esencial
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val noTieneInterfaz = launchIntent == null

        Log.d("AppMonitor", "esEsencial($packageName): $noTieneInterfaz")

        return noTieneInterfaz
    }

    private fun procesarPaquete(ninoId: String, nuevoPaquete: String) {
        val esencial = esEsencial(nuevoPaquete)

        if (esencial) {
            if (nuevoPaquete != paqueteEnPrimerPlano) {
                actualizarConsumoHastaAhora()
                finalizarSesionActiva()
                scope.launch { sincronizarConsumoSeguro() }
                paqueteEnPrimerPlano = nuevoPaquete
                ultimoTick = SystemClock.elapsedRealtime()
            }
            return
        }

        if (nuevoPaquete != paqueteEnPrimerPlano) {
            actualizarConsumoHastaAhora()
            finalizarSesionActiva()
            scope.launch { sincronizarConsumoSeguro() }
            paqueteEnPrimerPlano = nuevoPaquete
            ultimoTick = SystemClock.elapsedRealtime()

            val esHorarioSueno = HorarioSuenoUtils.estaEnHorarioSueno(horarioSueno)
            // Iniciar sesión si está permitida, tiene tiempo y no es horario de sueño
            if (appsPermitidas.contains(nuevoPaquete) && tieneTiempo(nuevoPaquete) && !esHorarioSueno) {
                iniciarSesion(ninoId, nuevoPaquete)
            }
        }

        // Evaluación de bloqueo
        val esAppPermitida = appsPermitidas.contains(nuevoPaquete)
        val msDisponibles = tiempoDisponible(nuevoPaquete)
        val esHorarioSueno = HorarioSuenoUtils.estaEnHorarioSueno(horarioSueno)

        Log.d("AppMonitor", "Evaluando $nuevoPaquete: Permitida=$esAppPermitida, TiempoRestante=$msDisponibles, Sueno=$esHorarioSueno")

        if (!esAppPermitida || msDisponibles <= 0 || esHorarioSueno) {
            Log.w("AppMonitor", "BLOQUEO ACTIVADO para $nuevoPaquete")
            finalizarSesionActiva()
            paqueteEnPrimerPlano = null
            if (esHorarioSueno && esAppPermitida) mostrarMensajeHorarioSueno(nuevoPaquete) else mostrarBloqueo(nuevoPaquete)
        }
    }

    @Suppress("DEPRECATION")
    private fun obtenerPaqueteEnPrimerPlano(): String? {
        rootInActiveWindow?.packageName?.toString()?.let { return it }
        val fin = System.currentTimeMillis()
        val eventos = usageStatsManager.queryEvents(fin - VENTANA_EVENTOS_MS, fin)
        val evento = UsageEvents.Event()
        var paquete: String? = null
        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(evento)
            if (evento.eventType == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) UsageEvents.Event.ACTIVITY_RESUMED else UsageEvents.Event.MOVE_TO_FOREGROUND) paquete = evento.packageName
        }
        return paquete
    }

    private fun recompensasEfectivas(packageName: String): List<Recompensa> = recompensas
        .filter { it.packageName == packageName }
        .map { recompensa ->
            recompensa.copy(
                milisegundosConsumidos = maxOf(
                    recompensa.milisegundosConsumidos,
                    consumosConfirmados[recompensa.id] ?: 0
                ) + (consumosPendientes[recompensa.id] ?: 0)
            )
        }

    private fun actualizarConsumoHastaAhora(): Long {
        val ahora = SystemClock.elapsedRealtime()
        val transcurrido = (ahora - ultimoTick).coerceAtLeast(0)
        ultimoTick = ahora
        val paquete = paqueteEnPrimerPlano
        if (paquete != null && appsPermitidas.contains(paquete)) consumir(paquete, transcurrido)
        return transcurrido
    }

    private fun consumir(packageName: String, milisegundos: Long) {
        ConsumoRecompensas.asignar(recompensasEfectivas(packageName), milisegundos).forEach { (id, consumo) ->
            consumosPendientes[id] = (consumosPendientes[id] ?: 0) + consumo
        }
    }

    private fun tiempoDisponible(packageName: String): Long =
        ConsumoRecompensas.disponibles(recompensasEfectivas(packageName))

    private fun tieneTiempo(packageName: String): Boolean = tiempoDisponible(packageName) > 0

    private suspend fun sincronizarConsumoSeguro() {
        runCatching { sincronizarConsumo() }
            .onFailure { Log.e("AppMonitor", "No se pudo sincronizar el consumo", it) }
    }

    private suspend fun sincronizarConsumo() {
        if (sincronizando || consumosPendientes.isEmpty()) return
        sincronizando = true
        val consumos = consumosPendientes.toMap()
        val objetivos = consumos.mapValues { (id, consumo) ->
            val remoto = recompensas.firstOrNull { it.id == id }?.milisegundosConsumidos ?: 0
            maxOf(remoto, consumosConfirmados[id] ?: 0) + consumo
        }
        try {
            repository.consumirRecompensas(consumos)
            consumos.forEach { (id, consumo) ->
                consumosConfirmados[id] = maxOf(consumosConfirmados[id] ?: 0, objetivos.getValue(id))
                consumosPendientes[id] = ((consumosPendientes[id] ?: 0) - consumo).coerceAtLeast(0)
                if (consumosPendientes[id] == 0L) consumosPendientes.remove(id)
            }
        } finally {
            sincronizando = false
        }
    }

    private fun iniciarSesion(ninoId: String, packageName: String) {
        if (sesionActiva?.packageName == packageName) return
        val datos = datosApps[packageName] ?: return
        val nombre = AppsUtils.obtenerNombreApp(this, packageName, datos.first)
        val sesion = SesionApp(UUID.randomUUID().toString(), ninoId, datos.second, packageName, nombre, System.currentTimeMillis())
        sesionActiva = sesion
        scope.launch { runCatching { estadisticasRepository.iniciarSesion(sesion) }.onFailure { Log.e("AppMonitor", "No se pudo iniciar sesión", it) } }
    }

    private fun finalizarSesionActiva() {
        val sesion = sesionActiva ?: return
        sesionActiva = null
        val fin = System.currentTimeMillis()
        scope.launch { runCatching { estadisticasRepository.finalizarSesion(sesion.id, fin, fin - sesion.inicio) }.onFailure { Log.e("AppMonitor", "No se pudo finalizar sesión", it) } }
    }

    private fun mostrarBloqueo(packageName: String) {
        val nombreGuardado = datosApps[packageName]?.first
        val nombreApp = AppsUtils.obtenerNombreApp(this, packageName, nombreGuardado)
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, "Canjea tiempo para usar $nombreApp", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarMensajeHorarioSueno(packageName: String) {
        val nombreGuardado = datosApps[packageName]?.first
        val nombreApp = AppsUtils.obtenerNombreApp(this, packageName, nombreGuardado)
        performGlobalAction(GLOBAL_ACTION_HOME)
        Toast.makeText(this, "$nombreApp no está disponible: es horario de sueño", Toast.LENGTH_LONG).show()
    }

    private fun notificarPocoTiempo(packageName: String, msDisponibles: Long) {
        NotificacionesUtils.crearCanales(this)
        val nombreGuardado = datosApps[packageName]?.first
        val nombreApp = AppsUtils.obtenerNombreApp(this, packageName, nombreGuardado)
        val minutos = (msDisponibles / 60000).toInt()
        NotificacionesUtils.notificar(
            this,
            NotificacionesUtils.CANAL_USO_APPS,
            "Te quedan $minutos minutos",
            "Tu tiempo para usar $nombreApp está por acabarse. Canjea más minutos si lo necesitas.",
            idNotificacion5Min
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        actualizarConsumoHastaAhora()
        finalizarSesionActiva()
        listenerApps?.remove()
        listenerRecompensas?.remove()
        listenerNino?.remove()
        val pendientes = consumosPendientes.toMap()
        if (pendientes.isNotEmpty()) CoroutineScope(Dispatchers.IO).launch {
            repository.consumirRecompensas(pendientes)
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val INTERVALO_MONITOR_MS = 250L
        private const val INTERVALO_SINCRONIZACION_MS = 5_000L
        private const val VENTANA_EVENTOS_MS = 5_000L
    }
}
