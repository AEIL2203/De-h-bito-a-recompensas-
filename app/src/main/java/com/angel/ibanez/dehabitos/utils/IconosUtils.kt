package com.angel.ibanez.dehabitos.utils

import android.content.Context
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.utils.*

object IconosUtils {

    fun crearIcono(
        context: Context,
        icono: String,
        colorRes: Int,
        sizeDp: Int = 24
    ): IconicsDrawable {
        return IconicsDrawable(context, icono).apply {
            color = IconicsColor.colorRes(colorRes)
            size = IconicsSize.dp(sizeDp)
        }
    }
}
