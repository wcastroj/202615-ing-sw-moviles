package com.example.vinyls_application.models

/** Géneros que acepta el backend. [apiValue] es el texto exacto que se muestra y se envía. */
enum class Genre(val apiValue: String) {
    CLASSICAL("Classical"),
    SALSA("Salsa"),
    ROCK("Rock"),
    FOLK("Folk");

    companion object {
        /** Se calcula una sola vez y la reutilizan todas las aperturas del formulario. */
        val apiValues: List<String> = entries.map { it.apiValue }
    }
}
