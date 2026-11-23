package com.example.vinyls_application.models

/** Sellos discográficos que acepta el backend. [apiValue] es el texto exacto que se muestra y se envía. */
enum class RecordLabel(val apiValue: String) {
    SONY_MUSIC("Sony Music"),
    EMI("EMI"),
    DISCOS_FUENTES("Discos Fuentes"),
    ELEKTRA("Elektra"),
    FANIA_RECORDS("Fania Records");

    companion object {
        /** Se calcula una sola vez y la reutilizan todas las aperturas del formulario. */
        val apiValues: List<String> = entries.map { it.apiValue }
    }
}
