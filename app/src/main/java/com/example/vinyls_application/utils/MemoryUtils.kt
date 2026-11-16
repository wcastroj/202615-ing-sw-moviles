package com.example.vinyls_application.utils

import android.app.ActivityManager
import android.content.Context
import com.bumptech.glide.load.DecodeFormat

/**
 * Utilidad de consulta y adaptación a la memoria disponible del dispositivo y la JVM.
 * Permite tomar decisiones dinámicas (como la selección del formato de decodificación de imágenes en Glide)
 * para reducir el uso de memoria en dispositivos o momentos con baja RAM disponible.
 */
object MemoryUtils {

    /**
     * Consulta si el sistema Android reporta que el dispositivo está en un estado de memoria baja.
     */
    fun isLowMemoryDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }

    /**
     * Calcula la proporción de memoria libre en el heap del Runtime de la JVM (retorna valor de 0.0 a 1.0).
     */
    fun getFreeMemoryRatio(): Double {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val allocatedMemory = runtime.totalMemory() - runtime.freeMemory()
        val freeMemory = maxMemory - allocatedMemory
        return freeMemory.toDouble() / maxMemory.toDouble()
    }

    /**
     * Determina el formato de decodificación de imágenes según el estado de la memoria:
     * Si la memoria es restringida (< 20% libre o dispositivo con lowMemory), utiliza
     * PREFER_RGB_565 (2 bytes por píxel), reduciendo el consumo de RAM de los Bitmaps en un 50%.
     * En caso contrario, utiliza PREFER_ARGB_8888 (4 bytes por píxel).
     */
    fun getPreferredDecodeFormat(context: Context): DecodeFormat {
        return if (isLowMemoryDevice(context) || getFreeMemoryRatio() < 0.20) {
            DecodeFormat.PREFER_RGB_565
        } else {
            DecodeFormat.PREFER_ARGB_8888
        }
    }
}
