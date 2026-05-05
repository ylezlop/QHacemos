package com.example.qhacemos.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qhacemos.datos.ResultadoEventos
import com.example.qhacemos.datos.cargarEventos
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.navigation.AppScreens
import java.time.format.TextStyle
import java.util.Locale

private const val CATEGORIA_TODOS = "Todos"
private const val FILTRO_PRECIO_TODOS = "Todos"
private val CATEGORIAS_EVENTOS = listOf(
    CATEGORIA_TODOS,
    "Arte y Cultura",
    "Vida Nocturna y Musica",
    "Talleres y Recreacion",
    "Mercaditos y Bazares Locales",
    "Tradiciones y Festivales Comunitarios",
    "Otros"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(navController: NavController) {
    val contexto = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    var listaEventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var cargandoEventos by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var intentoCarga by remember { mutableStateOf(0) }
    var busqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf(CATEGORIA_TODOS) }
    var filtroPrecio by remember { mutableStateOf(FILTRO_PRECIO_TODOS) }
    var filtroFecha by remember { mutableStateOf("") }
    var filtroUbicacion by remember { mutableStateOf("") }
    var mostrarFiltros by remember { mutableStateOf(false) }

    LaunchedEffect(contexto, intentoCarga) {
        cargandoEventos = true
        mensajeError = null

        when (val resultado = cargarEventos(contexto)) {
            is ResultadoEventos.Exito -> {
                listaEventos = resultado.eventos.filter { it.esVisibleParaUsuarios() }
            }

            is ResultadoEventos.Error -> {
                mensajeError = resultado.mensaje
                listaEventos = resultado.eventosLocales.filter { it.esVisibleParaUsuarios() }
            }
        }
        cargandoEventos = false
    }

    Scaffold(
        bottomBar = {
            BarraNavegacionInferior(
                navController = navController,
                rutaActual = backStackEntry?.destination?.route
            )
        }
    ) { valoresPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(valoresPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SeccionSuperior()
            }
            item {
                SeccionBarraBusqueda(
                    busqueda = busqueda,
                    onBusquedaChange = { busqueda = it },
                    onFiltrosClick = { mostrarFiltros = true }
                )
            }
            item {
                SeccionCategorias(
                    categoriaSeleccionada = categoriaSeleccionada,
                    onCategoriaSeleccionada = { categoriaSeleccionada = it }
                )
            }

            mensajeError?.let { error ->
                item {
                    MensajeErrorEventos(
                        mensaje = error,
                        onRetry = { intentoCarga++ }
                    )
                }
            }

            if (cargandoEventos) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF03A9F4))
                    }
                }
            } else {
                val eventosFiltrados = listaEventos.filtrarEventos(
                    busqueda = busqueda,
                    categoria = categoriaSeleccionada,
                    precio = filtroPrecio,
                    fecha = filtroFecha,
                    ubicacion = filtroUbicacion
                )

                item {
                    val destacados = eventosFiltrados.filter { it.esDestacado }
                    if (destacados.isNotEmpty()) {
                        SeccionEventosDestacados(
                            eventos = destacados,
                            onEventoClick = { evento ->
                                navController.navigate("event_detail/${evento.id}")
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = "Próximos eventos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val proximos = eventosFiltrados.filter { !it.esDestacado }

                if (eventosFiltrados.isEmpty()) {
                    item {
                        MensajeSinResultados(
                            hayFiltros = hayFiltrosActivos(
                                busqueda,
                                categoriaSeleccionada,
                                filtroPrecio,
                                filtroFecha,
                                filtroUbicacion
                            ),
                            onLimpiar = {
                                busqueda = ""
                                categoriaSeleccionada = CATEGORIA_TODOS
                                filtroPrecio = FILTRO_PRECIO_TODOS
                                filtroFecha = ""
                                filtroUbicacion = ""
                            }
                        )
                    }
                } else {
                    items(proximos) { evento ->
                        TarjetaProximoEvento(
                            evento = evento,
                            onClick = {
                                navController.navigate("event_detail/${evento.id}")
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (mostrarFiltros) {
        DialogoFiltrosEventos(
            precioSeleccionado = filtroPrecio,
            fecha = filtroFecha,
            ubicacion = filtroUbicacion,
            onPrecioChange = { filtroPrecio = it },
            onFechaChange = { filtroFecha = it },
            onUbicacionChange = { filtroUbicacion = it },
            onDismiss = { mostrarFiltros = false },
            onLimpiar = {
                filtroPrecio = FILTRO_PRECIO_TODOS
                filtroFecha = ""
                filtroUbicacion = ""
            }
        )
    }
}

@Composable
fun SeccionSuperior() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                "Explora eventos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text("¿Qué hacemos hoy?", fontSize = 14.sp, color = Color.Gray)
        }

        Surface(color = Color(0xFFE1F5FE), shape = RoundedCornerShape(16.dp)) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    tint = Color(0xFF03A9F4),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Xalapa, Ver.",
                    color = Color(0xFF03A9F4),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Desplegar",
                    tint = Color(0xFF03A9F4)
                )
            }
        }
    }
}

@Composable
fun MensajeErrorEventos(
    mensaje: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFEBEE),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = mensaje,
                color = Color(0xFFB3261E),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("Reintentar", color = Color(0xFFB3261E))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeccionBarraBusqueda(
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    onFiltrosClick: () -> Unit
) {
    OutlinedTextField(
        value = busqueda,
        onValueChange = onBusquedaChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Buscar eventos, organizadores o lugares...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = Color.Gray
            )
        },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4FC3F7))
                    .clickable { onFiltrosClick() }
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Filtros", tint = Color.White)
            }
        },
        shape = RoundedCornerShape(32.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedBorderColor = Color(0xFF03A9F4),
            cursorColor = Color(0xFF03A9F4)
        ),
        singleLine = true
    )
}

@Composable
fun SeccionCategorias(
    categoriaSeleccionada: String,
    onCategoriaSeleccionada: (String) -> Unit
) {
    val categorias = CATEGORIAS_EVENTOS
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categorias) { categoria ->
            val estaSeleccionada = categoria == categoriaSeleccionada
            Surface(
                modifier = Modifier.clickable { onCategoriaSeleccionada(categoria) },
                shape = RoundedCornerShape(20.dp),
                color = if (estaSeleccionada) Color(0xFF4FC3F7) else Color.White,
                border = if (!estaSeleccionada) BorderStroke(
                    1.dp,
                    Color(0xFFE0E0E0)
                ) else null
            ) {
                Text(
                    text = categoria,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (estaSeleccionada) Color.White else Color.DarkGray,
                    fontWeight = if (estaSeleccionada) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DialogoFiltrosEventos(
    precioSeleccionado: String,
    fecha: String,
    ubicacion: String,
    onPrecioChange: (String) -> Unit,
    onFechaChange: (String) -> Unit,
    onUbicacionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onLimpiar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtros") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Precio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(FILTRO_PRECIO_TODOS, "Gratis", "Pago").forEach { opcion ->
                        val seleccionada = precioSeleccionado == opcion
                        Surface(
                            modifier = Modifier.clickable { onPrecioChange(opcion) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (seleccionada) Color(0xFF4FC3F7) else Color.White,
                            border = if (!seleccionada) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null
                        ) {
                            Text(
                                text = opcion,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (seleccionada) Color.White else Color.DarkGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = fecha,
                    onValueChange = onFechaChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fecha") },
                    placeholder = { Text("Ej. 29 Mar, 1 Abr, viernes") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = onUbicacionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ubicacion") },
                    placeholder = { Text("Lugar, colonia o ciudad") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onLimpiar) {
                Text("Limpiar")
            }
        }
    )
}

@Composable
fun MensajeSinResultados(
    hayFiltros: Boolean,
    onLimpiar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hayFiltros) {
                    "No encontramos eventos con esos terminos."
                } else {
                    "Aun no hay eventos proximos en esta area."
                },
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hayFiltros) {
                    "Prueba con una categoria distinta o flexibiliza tu busqueda."
                } else {
                    "Prueba ampliando el radio de busqueda."
                },
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (hayFiltros) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onLimpiar) {
                    Text("Limpiar filtros", color = Color(0xFF03A9F4))
                }
            }
        }
    }
}

fun hayFiltrosActivos(
    busqueda: String,
    categoria: String,
    precio: String,
    fecha: String,
    ubicacion: String
): Boolean {
    return busqueda.isNotBlank() ||
        categoria != CATEGORIA_TODOS ||
        precio != FILTRO_PRECIO_TODOS ||
        fecha.isNotBlank() ||
        ubicacion.isNotBlank()
}

fun List<Evento>.filtrarEventos(
    busqueda: String,
    categoria: String,
    precio: String,
    fecha: String,
    ubicacion: String
): List<Evento> {
    val busquedaNormalizada = busqueda.normalizarBusqueda()
    val fechaNormalizada = fecha.normalizarBusqueda()
    val ubicacionNormalizada = ubicacion.normalizarBusqueda()

    return filter { evento ->
        val coincideBusqueda = if (busquedaNormalizada.isBlank()) {
            true
        } else {
            listOf(
                evento.titulo,
                evento.descripcion,
                evento.organizadorNombre,
                evento.ubicacion,
                evento.direccionCompleta,
                evento.categoria
            ).joinToString(" ").normalizarBusqueda().contains(busquedaNormalizada)
        }

        val coincideCategoria = categoria == CATEGORIA_TODOS ||
            evento.perteneceACategoria(categoria)

        val coincidePrecio = when (precio) {
            "Gratis" -> evento.esGratis || evento.costoMxn <= 0.0
            "Pago" -> !evento.esGratis && evento.costoMxn > 0.0
            else -> true
        }

        val coincideFecha = fechaNormalizada.isBlank() ||
            evento.coincideConFecha(fechaNormalizada)

        val coincideUbicacion = ubicacionNormalizada.isBlank() ||
            listOf(evento.ubicacion, evento.direccionCompleta)
                .joinToString(" ")
                .normalizarBusqueda()
                .contains(ubicacionNormalizada)

        coincideBusqueda && coincideCategoria && coincidePrecio && coincideFecha && coincideUbicacion
    }
}

fun Evento.perteneceACategoria(categoriaUi: String): Boolean {
    val categoriaEvento = categoria.normalizarBusqueda()
    return when (categoriaUi) {
        "Arte y Cultura" -> categoriaEvento in listOf(
            "exposiciones", "teatro", "danza", "literatura", "arte y cultura"
        )

        "Vida Nocturna y Musica" -> categoriaEvento in listOf(
            "conciertos", "dj sets", "fiestas", "bares", "vida nocturna y musica"
        )

        "Talleres y Recreacion" -> categoriaEvento in listOf(
            "talleres", "clases", "pintura", "ceramica", "cocina", "recreacion"
        )

        "Mercaditos y Bazares Locales" -> categoriaEvento in listOf(
            "mercaditos", "bazares", "mercaditos y bazares locales"
        )

        "Tradiciones y Festivales Comunitarios" -> categoriaEvento in listOf(
            "tradiciones", "festivales", "ferias", "tradiciones y festivales comunitarios"
        )

        "Otros" -> categoriaEvento !in listOf(
            "exposiciones", "teatro", "danza", "literatura", "arte y cultura",
            "conciertos", "dj sets", "fiestas", "bares", "vida nocturna y musica",
            "talleres", "clases", "pintura", "ceramica", "cocina", "recreacion",
            "mercaditos", "bazares", "mercaditos y bazares locales",
            "tradiciones", "festivales", "ferias", "tradiciones y festivales comunitarios"
        )

        else -> true
    }
}

fun String.normalizarBusqueda(): String {
    return lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ñ", "n")
        .trim()
}

fun Evento.coincideConFecha(fechaNormalizada: String): Boolean {
    if (fechaNormalizada.isBlank()) return true

    val fechaParseada = fechaInicioParseada
    if (fechaParseada != null) {
        val locale = Locale("es", "MX")
        val textoFecha = listOf(
            fechaParseada.toLocalDate().toString(),
            "${fechaParseada.dayOfMonth} ${fechaParseada.month.getDisplayName(TextStyle.SHORT, locale)}",
            "${fechaParseada.dayOfMonth} ${fechaParseada.month.getDisplayName(TextStyle.FULL, locale)}",
            fechaParseada.month.getDisplayName(TextStyle.FULL, locale),
            fechaParseada.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
            fechaParseada.year.toString(),
            fechaHora
        ).joinToString(" ").normalizarBusqueda()

        return textoFecha.contains(fechaNormalizada)
    }

    return fechaHora.normalizarBusqueda().contains(fechaNormalizada)
}

@Composable
fun SeccionEventosDestacados(
    eventos: List<Evento>,
    onEventoClick: (Evento) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Destacados esta semana", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { }) { Text("Ver todo", color = Color(0xFF03A9F4)) }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(eventos) { evento ->
                TarjetaEventoDestacado(
                    evento = evento,
                    onClick = { onEventoClick(evento) }
                )
            }
        }
    }
}

@Composable
fun TarjetaEventoDestacado(
    evento: Evento,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(evento.colorFondo)
            ) {
                Surface(
                    color = if (evento.esGratis) Color(0xFF00BFA5) else Color(0xFFFF7A1A),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        evento.costoTexto,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.sp
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = evento.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${evento.fechaHora} - ${evento.ubicacion}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                if (evento.organizadorNombre.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = evento.organizadorNombre,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun TarjetaProximoEvento(
    evento: Evento,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(evento.colorFondo)
            ) {
                Surface(
                    color = if (evento.esGratis) Color(0xFF00BFA5) else Color(0xFFFF7A1A),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        evento.costoTexto,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = evento.titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (evento.organizadorNombre.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = evento.organizadorNombre,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = evento.fechaHora,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = evento.ubicacion,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BarraNavegacionInferior(
    navController: NavController,
    rutaActual: String?
) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = rutaActual == AppScreens.Home.route,
            onClick = {
                navController.navigate(AppScreens.Home.route) {
                    launchSingleTop = true
                    popUpTo(AppScreens.Home.route) { inclusive = false }
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Mapa"
                )
            },
            label = { Text("Mapa") },
            selected = false,
            onClick = { }
        )
        NavigationBarItem(
            icon = {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF03A9F4))
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear", tint = Color.White)
                }
            },
            label = { Text("Crear") },
            selected = false,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Cuenta") },
            label = { Text("Cuenta") },
            selected = rutaActual == AppScreens.Account.route,
            onClick = {
                navController.navigate(AppScreens.Account.route) {
                    launchSingleTop = true
                }
            }
        )
    }
}
