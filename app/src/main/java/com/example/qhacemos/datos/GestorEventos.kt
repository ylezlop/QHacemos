package com.example.qhacemos.datos

import android.content.Context
import com.example.qhacemos.modelo.Evento
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jan.supabase.postgrest.from
import java.io.InputStreamReader

private const val TABLA_EVENTOS = "eventos"

sealed class ResultadoEventos {
    data class Exito(val eventos: List<Evento>) : ResultadoEventos()
    data class Error(val mensaje: String, val eventosLocales: List<Evento>) : ResultadoEventos()
}

// Necesitamos el 'Context' para poder acceder a la carpeta de recursos (assets) de la app.
fun leerEventos(contexto: Context): List<Evento> {
    return try {
        // Abrimos el archivo
        val flujoEntrada = contexto.assets.open("eventos.json")
        val lector = InputStreamReader(flujoEntrada)

        // Le indicamos a Gson que queremos transformar el JSON en una Lista de Eventos
        val tipoLista = object : TypeToken<List<Evento>>() {}.type

        // Hacemos la magia de conversión
        Gson().fromJson(lector, tipoLista)

    } catch (e: Exception) {
        // Si el archivo no existe o hay un error, imprimimos el error y devolvemos una lista vacía
        e.printStackTrace()
        emptyList()
    }
}

suspend fun leerEventosSupabase(): List<Evento> {
    if (!SupabaseCliente.estaConfigurado) return emptyList()

    return SupabaseCliente.cliente
        .from(TABLA_EVENTOS)
        .select()
        .decodeList<Evento>()
}

suspend fun obtenerEventos(contexto: Context): List<Evento> {
    return when (val resultado = cargarEventos(contexto)) {
        is ResultadoEventos.Exito -> resultado.eventos
        is ResultadoEventos.Error -> resultado.eventosLocales
    }
}

suspend fun cargarEventos(contexto: Context): ResultadoEventos {
    return try {
        val eventosRemotos = leerEventosSupabase()
        ResultadoEventos.Exito(eventosRemotos.ifEmpty { leerEventos(contexto) })
    } catch (e: Exception) {
        e.printStackTrace()
        ResultadoEventos.Error(
            mensaje = "Error al cargar los eventos. Revisa tu conexion e intenta de nuevo.",
            eventosLocales = leerEventos(contexto)
        )
    }
}
