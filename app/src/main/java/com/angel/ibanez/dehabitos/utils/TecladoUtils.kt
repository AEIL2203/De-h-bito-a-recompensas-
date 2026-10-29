package com.angel.ibanez.dehabitos.utils

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * Hace que la tecla de acción del teclado funcione en formularios:
 * en cada campo "Siguiente" salta al próximo y en el último "Listo" oculta
 * el teclado y ejecuta [alTerminar] (p. ej. pulsar el botón de enviar).
 */
object TecladoUtils {

    fun configurar(vararg campos: EditText, alTerminar: () -> Unit = {}) {
        campos.forEachIndexed { indice, campo ->
            val esUltimo = indice == campos.lastIndex
            campo.imeOptions = if (esUltimo) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
            campo.setOnEditorActionListener { _, actionId, evento ->
                val esEnter = evento?.keyCode == KeyEvent.KEYCODE_ENTER && evento.action == KeyEvent.ACTION_DOWN
                val esAccion = actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT ||
                    actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND
                if (!esAccion && !esEnter) return@setOnEditorActionListener false
                if (esUltimo) {
                    ocultar(campo)
                    campo.clearFocus()
                    alTerminar()
                } else {
                    campos[indice + 1].requestFocus()
                }
                true
            }
        }
    }

    fun ocultar(vista: View) {
        val imm = vista.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(vista.windowToken, 0)
    }
}
