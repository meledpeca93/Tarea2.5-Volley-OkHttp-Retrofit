package com.melvinpena.comparativa.data.remote

/** Rutas fijas compartidas por los tres clientes del laboratorio. */
enum class EscenarioLaboratorio(val etiqueta: String, val ruta: String) {
    NORMAL("Listado normal", "api/registros"),
    DEMORA("Tiempo de espera (demora de 15 s)", "api/pruebas/demora"),
    ERROR("Error interno (HTTP 500)", "api/pruebas/error");

    override fun toString() = "$etiqueta · GET /$ruta"
}
