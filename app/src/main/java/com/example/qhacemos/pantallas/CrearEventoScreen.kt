package com.example.qhacemos.pantallas

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.qhacemos.BuildConfig
import com.example.qhacemos.datos.GestorEventosOrganizador
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.modelo.PerfilUsuario
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale

private const val ZONA_HORARIA_EVENTOS = "-06:00"
private val ZONA_EVENTOS = ZoneId.of("America/Mexico_City")
private val UBICACION_XALAPA = LatLng(19.5438, -96.9102)
private val CATEGORIAS_FORMULARIO = listOf(
    "Arte y Cultura",
    "Vida Nocturna y Musica",
    "Talleres y Recreacion",
    "Mercaditos y Bazares Locales",
    "Tradiciones y Festivales Comunitarios",
    "Otros"
)
private val COLORES_EVENTOS = listOf(
    "#29B6F6",
    "#7E57C2",
    "#F57C00",
    "#4FC3F7",
    "#26A69A",
    "#EF5350",
    "#AB47BC"
)

private data class DireccionSugerida(
    val texto: String,
    val coordenadas: LatLng
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEventoScreen(
    navController: NavController,
    perfilActual: PerfilUsuario?,
    eventoId: Long? = null
) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val esEdicion = eventoId != null

    var eventoOriginal by remember { mutableStateOf<Evento?>(null) }
    var cargandoEvento by remember { mutableStateOf(esEdicion) }
    var mensajeCarga by remember { mutableStateOf<String?>(null) }

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var direccionCompleta by remember { mutableStateOf("") }
    var coordenadasSeleccionadas by remember { mutableStateOf<LatLng?>(null) }
    var categoria by remember { mutableStateOf(CATEGORIAS_FORMULARIO.first()) }
    var menuCategoriasAbierto by remember { mutableStateOf(false) }
    var contactoOrganizador by remember { mutableStateOf("") }
    var costoTexto by remember { mutableStateOf("0") }
    var colorHex by remember { mutableStateOf(COLORES_EVENTOS.random()) }
    var imagenesSeleccionadas by remember { mutableStateOf<List<String>>(emptyList()) }
    var sugerenciasDireccion by remember { mutableStateOf<List<DireccionSugerida>>(emptyList()) }
    var buscandoDireccion by remember { mutableStateOf(false) }
    var mostrarMapa by remember { mutableStateOf(true) }

    var mostrandoConfirmacion by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val posicionMapa = coordenadasSeleccionadas ?: UBICACION_XALAPA
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(posicionMapa, if (coordenadasSeleccionadas == null) 12f else 16f)
    }

    val selectorImagenes = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        imagenesSeleccionadas = uris.map { it.toString() }
    }

    LaunchedEffect(coordenadasSeleccionadas) {
        coordenadasSeleccionadas?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 16f))
        }
    }

    LaunchedEffect(eventoId) {
        if (eventoId == null) return@LaunchedEffect

        cargandoEvento = true
        mensajeCarga = null

        GestorEventosOrganizador.obtenerEventoPorId(eventoId)
            .onSuccess { evento ->
                if (evento == null) {
                    mensajeCarga = "No encontramos este evento."
                } else if (perfilActual != null && evento.organizadorId != perfilActual.id) {
                    mensajeCarga = "Solo puedes editar eventos creados por tu cuenta."
                } else {
                    eventoOriginal = evento
                    titulo = evento.titulo
                    descripcion = evento.descripcion
                    val fechaHora = evento.fechaInicio.aFechaHoraFormulario()
                    fecha = fechaHora.first
                    hora = fechaHora.second
                    ubicacion = evento.ubicacion
                    direccionCompleta = evento.direccionCompleta
                    coordenadasSeleccionadas = if (evento.latitud != null && evento.longitud != null) {
                        LatLng(evento.latitud, evento.longitud)
                    } else {
                        null
                    }
                    categoria = evento.categoria.takeIf { it in CATEGORIAS_FORMULARIO }
                        ?: CATEGORIAS_FORMULARIO.first()
                    contactoOrganizador = evento.contactoOrganizador
                    costoTexto = evento.costoMxn.toString()
                    colorHex = evento.colorHex.ifBlank { colorHex }
                    imagenesSeleccionadas = evento.imagenes.ifEmpty {
                        evento.imagenPrincipalUrl.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
                    }
                }
            }
            .onFailure { error ->
                mensajeCarga = error.message ?: "No se pudo cargar el evento."
            }

        cargandoEvento = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Editar evento" else "Crear evento") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (cargandoEvento) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                return@Column
            }

            mensajeCarga?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Button(onClick = { navController.popBackStack() }) {
                    Text("Volver")
                }
                return@Column
            }

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Titulo del evento *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripcion *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text("Fecha *") },
                    placeholder = { Text("dd/mm/aaaa") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hora,
                    onValueChange = { hora = it },
                    label = { Text("Hora *") },
                    placeholder = { Text("18:30") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = ubicacion,
                onValueChange = { ubicacion = it },
                label = { Text("Lugar o sede *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = direccionCompleta,
                onValueChange = {
                    direccionCompleta = it
                    sugerenciasDireccion = emptyList()
                },
                label = { Text("Direccion completa *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = {
                    scope.launch {
                        buscandoDireccion = true
                        sugerenciasDireccion = buscarDirecciones(contexto, direccionCompleta)
                        buscandoDireccion = false

                        if (sugerenciasDireccion.isEmpty()) {
                            Toast.makeText(contexto, "No se encontraron direcciones.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = direccionCompleta.isNotBlank() && !buscandoDireccion,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (buscandoDireccion) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Buscar direccion")
                }
            }

            sugerenciasDireccion.forEach { sugerencia ->
                OutlinedButton(
                    onClick = {
                        direccionCompleta = sugerencia.texto
                        coordenadasSeleccionadas = sugerencia.coordenadas
                        sugerenciasDireccion = emptyList()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(sugerencia.texto)
                }
            }

            if (BuildConfig.MAPS_API_KEY.isBlank()) {
                Text(
                    text = "Configura MAPS_API_KEY en local.properties para mostrar el mapa.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (!tieneGooglePlayServices(contexto)) {
                Text(
                    text = "Google Play Services no esta disponible para mostrar el mapa en este dispositivo.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (mostrarMapa || coordenadasSeleccionadas != null) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    cameraPositionState = cameraPositionState,
                    onMapClick = {
                        coordenadasSeleccionadas = it
                    }
                ) {
                    coordenadasSeleccionadas?.let { posicion ->
                        Marker(
                            state = MarkerState(position = posicion),
                            title = titulo.ifBlank { "Evento" }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona la categoria de tu evento") },
                    trailingIcon = {
                        IconButton(onClick = { menuCategoriasAbierto = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Categorias")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { menuCategoriasAbierto = true },
                    singleLine = true
                )

                DropdownMenu(
                    expanded = menuCategoriasAbierto,
                    onDismissRequest = { menuCategoriasAbierto = false }
                ) {
                    CATEGORIAS_FORMULARIO.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                categoria = opcion
                                menuCategoriasAbierto = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = contactoOrganizador,
                onValueChange = { contactoOrganizador = it },
                label = { Text("Contacto del organizador") },
                placeholder = { Text("correo, telefono o red social") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = costoTexto,
                onValueChange = { costoTexto = it },
                label = { Text("Costo MXN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = { selectorImagenes.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (imagenesSeleccionadas.isEmpty()) {
                        "Seleccionar imagenes"
                    } else {
                        "${imagenesSeleccionadas.size} imagenes seleccionadas"
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val error = validarFormularioEvento(
                        titulo = titulo,
                        descripcion = descripcion,
                        fecha = fecha,
                        hora = hora,
                        ubicacion = ubicacion,
                        direccionCompleta = direccionCompleta,
                        coordenadasSeleccionadas = coordenadasSeleccionadas,
                        categoria = categoria,
                        costoTexto = costoTexto
                    )

                    if (error != null) {
                        Toast.makeText(contexto, error, Toast.LENGTH_LONG).show()
                    } else {
                        mostrandoConfirmacion = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (esEdicion) "Guardar cambios" else "Publicar evento")
            }
        }
    }

    if (mostrandoConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrandoConfirmacion = false },
            title = { Text(if (esEdicion) "Confirmar edicion" else "Confirmar publicacion") },
            text = {
                Text(
                    if (esEdicion) {
                        "El evento se actualizara y quedara pendiente de validacion."
                    } else {
                        "Se simulara el pago de publicacion y el evento quedara pendiente de validacion."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !guardando,
                    onClick = {
                        mostrandoConfirmacion = false
                        guardando = true
                        val evento = construirEventoFormulario(
                            eventoOriginal = eventoOriginal,
                            perfilActual = perfilActual,
                            titulo = titulo,
                            descripcion = descripcion,
                            fecha = fecha,
                            hora = hora,
                            ubicacion = ubicacion,
                            direccionCompleta = direccionCompleta,
                            coordenadasSeleccionadas = coordenadasSeleccionadas,
                            categoria = categoria,
                            contactoOrganizador = contactoOrganizador,
                            costoTexto = costoTexto,
                            colorHex = colorHex,
                            imagenes = imagenesSeleccionadas
                        )

                        scope.launch {
                            val resultado = if (esEdicion) {
                                GestorEventosOrganizador.actualizarEvento(evento)
                            } else {
                                GestorEventosOrganizador.publicarEvento(evento, 50.0)
                            }
                            guardando = false

                            resultado
                                .onSuccess {
                                    Toast.makeText(
                                        contexto,
                                        if (esEdicion) "Evento actualizado" else "Evento guardado. Pendiente de validacion",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.popBackStack()
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        contexto,
                                        error.message ?: "No se pudo guardar el evento",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                ) {
                    if (guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Confirmar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrandoConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun validarFormularioEvento(
    titulo: String,
    descripcion: String,
    fecha: String,
    hora: String,
    ubicacion: String,
    direccionCompleta: String,
    coordenadasSeleccionadas: LatLng?,
    categoria: String,
    costoTexto: String
): String? {
    if (titulo.isBlank() || descripcion.isBlank() || fecha.isBlank() || hora.isBlank() ||
        ubicacion.isBlank() || direccionCompleta.isBlank() || categoria.isBlank()
    ) {
        return "Completa los campos obligatorios."
    }

    if (fechaHoraIso(fecha, hora) == null) {
        return "Usa fecha dd/mm/aaaa y hora HH:mm."
    }

    if (coordenadasSeleccionadas == null) {
        return "Busca una direccion o marca la ubicacion en el mapa."
    }

    if (costoTexto.toDoubleOrNull() == null) {
        return "El costo debe ser numerico."
    }

    return null
}

private fun construirEventoFormulario(
    eventoOriginal: Evento?,
    perfilActual: PerfilUsuario?,
    titulo: String,
    descripcion: String,
    fecha: String,
    hora: String,
    ubicacion: String,
    direccionCompleta: String,
    coordenadasSeleccionadas: LatLng?,
    categoria: String,
    contactoOrganizador: String,
    costoTexto: String,
    colorHex: String,
    imagenes: List<String>
): Evento {
    val costo = costoTexto.toDoubleOrNull() ?: 0.0
    val imagenPrincipal = imagenes.firstOrNull().orEmpty()

    return Evento(
        id = eventoOriginal?.id ?: 0L,
        titulo = titulo.trim(),
        descripcion = descripcion.trim(),
        fechaInicio = fechaHoraIso(fecha, hora).orEmpty(),
        fechaHora = fechaHoraResumen(fecha, hora),
        ubicacion = ubicacion.trim(),
        direccionCompleta = direccionCompleta.trim(),
        latitud = coordenadasSeleccionadas?.latitude,
        longitud = coordenadasSeleccionadas?.longitude,
        categoria = categoria.trim(),
        organizadorId = eventoOriginal?.organizadorId ?: perfilActual?.id.orEmpty(),
        organizadorNombre = eventoOriginal?.organizadorNombre
            ?: perfilActual?.nombre?.ifBlank { perfilActual.email.substringBefore("@") }
            ?: "Organizador",
        contactoOrganizador = contactoOrganizador.trim(),
        costoMxn = costo,
        moneda = "MXN",
        esDestacado = eventoOriginal?.esDestacado ?: false,
        esGratis = costo <= 0.0,
        tipoPublicacion = eventoOriginal?.tipoPublicacion ?: "pago",
        estado = "pendiente_validacion",
        imagenPrincipalUrl = imagenPrincipal,
        imagenes = imagenes,
        vistas = eventoOriginal?.vistas ?: 0,
        clicks = eventoOriginal?.clicks ?: 0,
        guardados = eventoOriginal?.guardados ?: 0,
        colorHex = colorHex.trim()
    )
}

private suspend fun buscarDirecciones(
    context: Context,
    direccion: String
): List<DireccionSugerida> = withContext(Dispatchers.IO) {
    if (direccion.isBlank()) return@withContext emptyList()

    val consulta = if (direccion.contains("Xalapa", ignoreCase = true)) {
        direccion
    } else {
        "$direccion, Xalapa, Veracruz, Mexico"
    }

    runCatching {
        @Suppress("DEPRECATION")
        Geocoder(context, Locale("es", "MX"))
            .getFromLocationName(consulta, 5)
            .orEmpty()
            .filter { it.hasLatitude() && it.hasLongitude() }
            .map { direccionEncontrada ->
                DireccionSugerida(
                    texto = direccionEncontrada.getAddressLine(0) ?: consulta,
                    coordenadas = LatLng(direccionEncontrada.latitude, direccionEncontrada.longitude)
                )
            }
            .distinctBy { it.texto }
    }.getOrDefault(emptyList())
}

private fun tieneGooglePlayServices(context: Context): Boolean {
    return GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}

private fun fechaHoraIso(fecha: String, hora: String): String? {
    return try {
        val fechaLocal = LocalDate.parse(fecha.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val horaLocal = LocalTime.parse(hora.trim(), DateTimeFormatter.ofPattern("HH:mm"))
        OffsetDateTime.of(fechaLocal, horaLocal, ZoneOffset.of(ZONA_HORARIA_EVENTOS))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun fechaHoraResumen(fecha: String, hora: String): String {
    return try {
        val fechaLocal = LocalDate.parse(fecha.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val mes = fechaLocal.month.getDisplayName(TextStyle.SHORT, Locale("es", "MX"))
            .replace(".", "")
        "${fechaLocal.dayOfMonth} $mes - ${hora.trim()} hrs"
    } catch (_: DateTimeParseException) {
        "$fecha - $hora hrs"
    }
}

private fun String.aFechaHoraFormulario(): Pair<String, String> {
    if (isBlank()) return "" to ""

    return try {
        val fechaHora = OffsetDateTime.parse(this).atZoneSameInstant(ZONA_EVENTOS)
        val fecha = fechaHora.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val hora = fechaHora.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        fecha to hora
    } catch (_: DateTimeParseException) {
        "" to ""
    }
}
