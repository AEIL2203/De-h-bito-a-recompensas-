package com.angel.ibanez.dehabitos.utils

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.angel.ibanez.dehabitos.R

object TutorialManager {

    fun iniciarTutorialTutorSiNecesario(activity: FragmentActivity) {
        if (DispositivoConfig.tutorialVisto(DispositivoConfig.ModoDispositivo.PADRE) || TutorialOverlay.isShowing(activity)) return
        val pasos = pasosTutor(activity)
        TutorialOverlay.start(activity, pasos) {
            DispositivoConfig.marcarTutorialVisto(DispositivoConfig.ModoDispositivo.PADRE)
        }
    }

    private fun pasosTutor(activity: FragmentActivity): List<TutorialStep> {
        fun click(id: Int): () -> Unit = { activity.findViewById<View>(id)?.performClick() }
        fun back(): () -> Unit = { activity.onBackPressedDispatcher.onBackPressed() }
        fun primerItem(recyclerId: Int): () -> View? = {
            activity.findViewById<RecyclerView>(recyclerId)?.getChildAt(0)
        }

        return listOf(
            // --- INICIO ---
            TutorialStep(
                targetViewId = R.id.btnCerrarSesion,
                title = "Cerrar sesión",
                message = "Con este botón de la esquina superior derecha puedes cerrar la sesión actual cuando lo necesites.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM,
                onPresent = click(R.id.navTutorInicio)
            ),
            TutorialStep(
                targetViewId = R.id.tvMisNinos,
                title = "Mis niños",
                message = "En esta sección aparecen todos los perfiles infantiles que hayas registrado.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM,
                onPresent = click(R.id.navTutorInicio)
            ),
            TutorialStep(
                targetProvider = primerItem(R.id.rvNinos),
                title = "Perfil de un niño",
                message = "Toca sobre un niño para ver sus estadísticas, progreso y actividad reciente.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM,
                onPresent = click(R.id.navTutorInicio)
            ),

            // --- GESTIÓN ---
            TutorialStep(
                targetViewId = R.id.navTutorGestion,
                title = "Gestión",
                message = "Esta pestaña agrupa las opciones para administrar perfiles, dispositivos y descanso.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP,
                onPresent = click(R.id.navTutorGestion)
            ),
            TutorialStep(
                targetViewId = R.id.btnAgregarNino,
                title = "Agregar niño",
                message = "Crea un nuevo perfil infantil con nombre y edad para que pueda usar la app.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.btnGestionarDispositivos,
                title = "Gestionar dispositivos",
                message = "Vincula o desvincula los dispositivos que el niño utiliza y controla el acceso.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.btnHorarioSueno,
                title = "Horario de sueño",
                message = "Establece la hora de dormir y despertar, así como los días en que estará activo el descanso.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),

            // --- CONTROL ---
            TutorialStep(
                targetViewId = R.id.navTutorControl,
                title = "Control",
                message = "Desde aquí configuras apps, asignas misiones y validas las evidencias enviadas.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP,
                onPresent = click(R.id.navTutorControl)
            ),
            TutorialStep(
                targetViewId = R.id.btnConfigurarApps,
                title = "Configurar apps y precios",
                message = "Aquí eliges qué apps de entretenimiento estarán disponibles para el niño y cuántas estrellas cuesta usarlas.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            // Entrar a Configurar apps
            TutorialStep(
                targetViewId = R.id.tilNino,
                title = "Seleccionar niño",
                message = "Primero elige el perfil infantil al que le vas a configurar las apps.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM,
                onPresent = click(R.id.btnConfigurarApps)
            ),
            TutorialStep(
                targetViewId = R.id.btnActualizarCatalogo,
                title = "Actualizar catálogo",
                message = "Sincroniza la lista de apps instaladas en el dispositivo del niño.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.tilApp,
                title = "Elegir app",
                message = "Selecciona una app de entretenimiento de las que detectó el sistema.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.tilPrecio15,
                title = "Precios por tiempo",
                message = "Define cuántas estrellas cuesta usar la app durante 15, 30 o 60 minutos.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.btnAgregar,
                title = "Agregar app",
                message = "Guarda la app con sus precios para que el niño pueda canjearlas después.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.rvApps,
                title = "Apps configuradas",
                message = "Aquí verás todas las apps que ya has habilitado con sus respectivos precios.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP
            ),
            // Volver a Control y seguir explicando
            TutorialStep(
                targetViewId = R.id.btnAsignarTarea,
                title = "Asignar misión",
                message = "Crea misiones académicas, del hogar o personales, asigna puntos XP y, si quieres, una fecha límite.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM,
                onPresent = back()
            ),
            TutorialStep(
                targetViewId = R.id.btnValidarTareas,
                title = "Validar misiones",
                message = "Revisa las evidencias (fotos) que tus hijos envían y aprueba o rechaza cada misión.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),

            // --- CONFIGURACIÓN ---
            TutorialStep(
                targetViewId = R.id.navTutorConfiguracion,
                title = "Configuración",
                message = "En esta pestaña encuentras ajustes, encuesta, términos y cierre de sesión.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP,
                onPresent = click(R.id.navTutorConfiguracion)
            ),
            TutorialStep(
                targetViewId = R.id.btnEncuesta,
                title = "Encuesta de satisfacción",
                message = "Califica la experiencia de la app para ayudarnos a mejorar.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.btnTerminos,
                title = "Términos y condiciones",
                message = "Lee los términos de uso, privacidad y seguridad del sistema.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.btnConfigurarNotificaciones,
                title = "Notificaciones",
                message = "Activa o desactiva los avisos y define cuándo quieres recibirlos.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.btnPinParental,
                title = "PIN parental",
                message = "Crea o cambia un PIN de 6 dígitos para proteger el acceso a configuraciones sensibles.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.switchModoOscuro,
                title = "Modo oscuro",
                message = "Cambia entre tema claro y oscuro según tu preferencia.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            )
        )
    }

    fun iniciarTutorialNinoSiNecesario(activity: FragmentActivity) {
        if (DispositivoConfig.tutorialVisto(DispositivoConfig.ModoDispositivo.HIJO) || TutorialOverlay.isShowing(activity)) return
        mostrarTutorialNino(activity)
    }

    fun mostrarTutorialTutor(activity: FragmentActivity) {
        if (TutorialOverlay.isShowing(activity)) return
        val pasos = pasosTutor(activity)
        TutorialOverlay.start(activity, pasos) {
            DispositivoConfig.marcarTutorialVisto(DispositivoConfig.ModoDispositivo.PADRE)
        }
    }

    fun mostrarTutorialNino(activity: FragmentActivity) {
        if (TutorialOverlay.isShowing(activity)) return
        val pasos = pasosNino(activity)
        TutorialOverlay.start(activity, pasos) {
            DispositivoConfig.marcarTutorialVisto(DispositivoConfig.ModoDispositivo.HIJO)
        }
    }

    private fun pasosNino(activity: FragmentActivity): List<TutorialStep> {
        fun click(id: Int): () -> Unit = { activity.findViewById<View>(id)?.performClick() }
        fun primerItem(recyclerId: Int, childId: Int? = null): () -> View? = {
            val rv = activity.findViewById<RecyclerView>(recyclerId)
            val item = rv?.getChildAt(0)
            if (childId != null) item?.findViewById<View>(childId) else item
        }

        return listOf(
            // --- INICIO ---
            TutorialStep(
                targetViewId = R.id.cardProfilePill,
                title = "Tu perfil",
                message = "Aquí aparecen tu nombre, nivel actual, experiencia (XP) y el avatar que te representa.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM,
                onPresent = click(R.id.navInicio)
            ),
            TutorialStep(
                targetViewId = R.id.containerCurrency,
                title = "Tus estrellas",
                message = "Son las estrellas que ganas al completar misiones. Úsalas para canjear tiempo de entretenimiento.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.cardEstadisticas,
                title = "Resumen por categoría",
                message = "Aquí ves cuántas misiones tienes en Tareas (académico), Hogar y Personal.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.cardStats,
                title = "Estrellas, racha e insignias",
                message = "Estrellas es tu moneda, Racha cuenta tus días seguidos y las Insignias son logros especiales.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.tvTituloLogros,
                title = "Logros",
                message = "Desbloquea logros al alcanzar niveles, acumular XP y completar retos.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),

            // --- CAMINO ---
            TutorialStep(
                targetViewId = R.id.navCamino,
                title = "Camino",
                message = "Este es tu mapa de niveles. Completa misiones para avanzar y desbloquear nuevas recompensas.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP,
                onPresent = click(R.id.navCamino)
            ),
            TutorialStep(
                targetViewId = R.id.gameMap,
                title = "Niveles",
                message = "Cada punto representa un nivel. Gana XP y subirás automáticamente por el camino.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),

            // --- MISIONES ---
            TutorialStep(
                targetViewId = R.id.navMisiones,
                title = "Misiones",
                message = "Aquí encontrarás las misiones que tu tutor te asigne y las misiones diarias.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP,
                onPresent = click(R.id.navMisiones)
            ),
            TutorialStep(
                targetProvider = primerItem(R.id.rvMisiones, R.id.btnCompletar),
                title = "Botón Lo hice",
                message = "Cuando termines una misión, toca este botón para enviar una foto como evidencia.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetProvider = primerItem(R.id.rvMisiones, R.id.tvNombreMision),
                title = "Misiones del tutor",
                message = "Tu tutor te asignará misiones personales con puntos y fecha límite.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.rvMisiones,
                title = "Misiones diarias",
                message = "También aparecen retos diarios que puedes completar para ganar más estrellas.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP
            ),

            // --- PREMIOS ---
            TutorialStep(
                targetViewId = R.id.navPremios,
                title = "Premios",
                message = "Canjea tus estrellas por tiempo para usar las apps de entretenimiento que tu tutor haya configurado.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP,
                onPresent = click(R.id.navPremios)
            ),
            TutorialStep(
                targetViewId = R.id.rvApps,
                title = "Tienda de premios",
                message = "Elige una app y el tiempo que quieres canjear. Si no aparece nada, pide a tu tutor que configure las apps.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP
            ),

            // --- PERFIL ---
            TutorialStep(
                targetViewId = R.id.navPerfil,
                title = "Perfil",
                message = "Aquí personalizas tu apariencia y pides ayuda a tu tutor para entrar al modo padre.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP,
                onPresent = click(R.id.navPerfil)
            ),
            TutorialStep(
                targetViewId = R.id.ivAvatar,
                title = "Tu avatar",
                message = "Esta imagen te representa en el juego. Puedes cambiarla más abajo.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.tvNombre,
                title = "Nombre y nivel",
                message = "Aquí se muestra tu nombre y el progreso de experiencia que llevas.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.switchModoOscuro,
                title = "Modo oscuro",
                message = "Activa o desactiva el tema oscuro para tu comodidad visual.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.switchMusica,
                title = "Música de fondo",
                message = "Enciende o apaga la música ambiental de la app.",
                tooltipPosition = TutorialStep.TooltipPosition.BOTTOM
            ),
            TutorialStep(
                targetViewId = R.id.rvAvatares,
                title = "Cambiar avatar",
                message = "Desplaza esta lista y toca el avatar que más te guste.",
                tooltipPosition = TutorialStep.TooltipPosition.TOP
            )
        )
    }
}
