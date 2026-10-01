package com.angel.ibanez.dehabitos

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.angel.ibanez.dehabitos.utils.IconosUtils
import com.angel.ibanez.dehabitos.utils.TemaConfig
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var cardNavTutor: MaterialCardView
    private lateinit var indicadorTutor: View
    private lateinit var botonesTutor: List<MaterialButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        TemaConfig.aplicar(this)
        super.onCreate(savedInstanceState)
        // El manifest usa el tema Launcher para mostrar el logo al arrancar.
        // Antes de inflar la UI volvemos al tema normal de la app.
        setTheme(R.style.Theme_DeHábitosARecompensas)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController
        cardNavTutor = findViewById(R.id.navTutorGlobal)
        indicadorTutor = findViewById(R.id.vIndicadorTutorGlobal)
        botonesTutor = listOf(
            findViewById(R.id.navTutorGlobalInicio),
            findViewById(R.id.navTutorGlobalGestion),
            findViewById(R.id.navTutorGlobalControl),
            findViewById(R.id.navTutorGlobalConfiguracion)
        )

        configurarIconosTutor()
        botonesTutor.forEachIndexed { indice, boton ->
            boton.setOnClickListener { abrirSeccionTutor(indice) }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val indice = indiceTutor(destination.id)
            cardNavTutor.visibility = if (indice != null && destination.id != R.id.dashboardTutorFragment) View.VISIBLE else View.GONE
            indice?.let { actualizarMenuTutor(it, false) }
        }
    }

    private fun configurarIconosTutor() {
        botonesTutor[0].icon = IconosUtils.crearIcono(this, "gmr-home", R.color.md_theme_primary)
        botonesTutor[1].icon = IconosUtils.crearIcono(this, "gmr-people", R.color.md_theme_primary)
        botonesTutor[2].icon = IconosUtils.crearIcono(this, "gmr-tune", R.color.md_theme_primary)
        botonesTutor[3].icon = IconosUtils.crearIcono(this, "gmr-settings", R.color.md_theme_primary)
    }

    private fun abrirSeccionTutor(indice: Int) {
        val opciones = navOptions {
            launchSingleTop = true
            popUpTo(R.id.dashboardTutorFragment) { inclusive = true }
        }
        navController.navigate(
            R.id.dashboardTutorFragment,
            Bundle().apply { putInt("tutorTab", indice) },
            opciones
        )
    }

    private fun actualizarMenuTutor(indice: Int, animar: Boolean) {
        botonesTutor.forEachIndexed { posicion, boton ->
            val seleccionado = posicion == indice
            val color = MaterialColors.getColor(
                boton,
                if (seleccionado) com.google.android.material.R.attr.colorOnPrimaryContainer
                else com.google.android.material.R.attr.colorOnSurfaceVariant,
                0
            )
            boton.setTextColor(color)
            boton.iconTint = ColorStateList.valueOf(color)
        }
        val boton = botonesTutor[indice]
        boton.post {
            if (indicadorTutor.width == 0) {
                indicadorTutor.layoutParams = indicadorTutor.layoutParams.apply {
                    width = boton.width
                    height = boton.height
                }
                indicadorTutor.x = boton.x
                indicadorTutor.y = boton.y
                indicadorTutor.visibility = View.VISIBLE
            } else if (animar) {
                indicadorTutor.animate().withLayer()
                    .x(boton.x).y(boton.y)
                    .setDuration(250)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            } else {
                indicadorTutor.x = boton.x
                indicadorTutor.y = boton.y
            }
        }
    }

    private fun indiceTutor(destinationId: Int): Int? = when (destinationId) {
        R.id.dashboardTutorFragment -> 0
        R.id.agregarNinoFragment, R.id.dispositivosFragment -> 1
        R.id.estadisticasNinoFragment -> 0
        R.id.configurarAppsFragment, R.id.asignarTareaFragment,
        R.id.validarTareasFragment -> 2
        R.id.encuestaSatisfaccionFragment, R.id.terminosCondicionesFragment -> 3
        else -> null
    }
}
