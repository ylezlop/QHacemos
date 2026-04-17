package com.example.qhacemos.datos

import android.content.Context
import com.example.qhacemos.modelo.Evento
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

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