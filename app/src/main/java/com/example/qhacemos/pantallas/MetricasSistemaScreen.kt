package com.example.qhacemos.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.qhacemos.datos.FiltrosMetricas
import com.example.qhacemos.datos.GestorMetricas
import com.example.qhacemos.datos.MetricasSistema
import com.example.qhacemos.datos.PuntoGrafica
import java.util.Locale

private val ESTADOS_METRICAS = listOf("Todos", "publicado", "pendiente_validacion", "eliminado")
private val PERIODOS_METRICAS = listOf("Todo", "Ultimos 30 dias", "Ultimos 90 dias")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricasSistemaScreen(navController: NavController) {
    val context = LocalContext.current
    var filtros by remember { mutableStateOf(FiltrosMetricas()) }
    var metricas by remember { mutableStateOf<MetricasSistema?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var intentoCarga by remember { mutableStateOf(0) }

    LaunchedEffect(context, filtros, intentoCarga) {
        cargando = true
        mensajeError = null

        GestorMetricas.obtenerMetricasSistema(context, filtros)
            .onSuccess { metricas = it }
            .onFailure { error ->
                metricas = null
                mensajeError = error.message ?: "No se pudieron cargar las metricas."
            }

        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metricas globales") },
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
                .fillMaxSize()
                .background(Color(0xFFF4F7FB))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            metricas?.let { datos ->
                FiltrosMetricasPanel(
                    filtros = filtros,
                    categorias = datos.categoriasDisponibles,
                    organizadores = datos.organizadoresDisponibles,
                    onFiltrosChange = { filtros = it }
                )
            }

            if (cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            mensajeError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Button(onClick = { intentoCarga++ }) {
                    Text("Reintentar")
                }
            }

            metricas?.let { datos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TarjetaMetrica("Usuarios", datos.totalUsuarios.toString(), Modifier.weight(1f))
                    TarjetaMetrica("Eventos", datos.totalEventos.toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TarjetaMetrica("Publicados", datos.eventosPublicados.toString(), Modifier.weight(1f))
                    TarjetaMetrica("Pendientes", datos.eventosPendientes.toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TarjetaMetrica("Destacados", datos.eventosDestacados.toString(), Modifier.weight(1f))
                    TarjetaMetrica("Suscripciones", datos.suscripcionesActivas.toString(), Modifier.weight(1f))
                }

                GraficaBarras(
                    titulo = "Interacciones",
                    datos = datos.interacciones,
                    color = Color(0xFF0277BD)
                )

                GraficaBarras(
                    titulo = "Eventos por categoria",
                    datos = datos.eventosPorCategoria,
                    color = Color(0xFF2E7D32)
                )

                GraficaBarras(
                    titulo = "Eventos por organizador",
                    datos = datos.eventosPorOrganizador,
                    color = Color(0xFF6A1B9A)
                )

                TarjetaMetrica("Vistas totales", datos.vistasTotales.toString(), Modifier.fillMaxWidth())
                TarjetaMetrica("Clicks totales", datos.clicksTotales.toString(), Modifier.fillMaxWidth())
                TarjetaMetrica("Guardados totales", datos.guardadosTotales.toString(), Modifier.fillMaxWidth())
                TarjetaMetrica(
                    etiqueta = "Ingresos estimados",
                    valor = "$${String.format(Locale.US, "%.2f", datos.ingresosEstimadosMxn)} MXN",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FiltrosMetricasPanel(
    filtros: FiltrosMetricas,
    categorias: List<String>,
    organizadores: List<String>,
    onFiltrosChange: (FiltrosMetricas) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Filtros", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            SelectorFiltro(
                etiqueta = "Estado",
                valor = filtros.estado,
                opciones = ESTADOS_METRICAS,
                onSeleccion = { onFiltrosChange(filtros.copy(estado = it)) }
            )
            SelectorFiltro(
                etiqueta = "Categoria",
                valor = filtros.categoria,
                opciones = listOf("Todas") + categorias,
                onSeleccion = { onFiltrosChange(filtros.copy(categoria = it)) }
            )
            SelectorFiltro(
                etiqueta = "Organizador",
                valor = filtros.organizador,
                opciones = listOf("Todos") + organizadores,
                onSeleccion = { onFiltrosChange(filtros.copy(organizador = it)) }
            )
            SelectorFiltro(
                etiqueta = "Periodo",
                valor = filtros.periodo,
                opciones = PERIODOS_METRICAS,
                onSeleccion = { onFiltrosChange(filtros.copy(periodo = it)) }
            )
        }
    }
}

@Composable
private fun SelectorFiltro(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    onSeleccion: (String) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = {
                IconButton(onClick = { abierto = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = etiqueta)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { abierto = true },
            singleLine = true
        )
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GraficaBarras(
    titulo: String,
    datos: List<PuntoGrafica>,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            if (datos.isEmpty()) {
                Text("Sin datos para los filtros seleccionados.", color = Color.Gray, fontSize = 13.sp)
            } else {
                val maximo = datos.maxOf { it.valor }.coerceAtLeast(1)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val espacio = 12.dp.toPx()
                    val altoTexto = 28.dp.toPx()
                    val anchoBarra = (size.width - espacio * (datos.size + 1)) / datos.size.coerceAtLeast(1)
                    val altoDisponible = size.height - altoTexto

                    datos.forEachIndexed { index, punto ->
                        val altura = altoDisponible * (punto.valor.toFloat() / maximo)
                        val izquierda = espacio + index * (anchoBarra + espacio)
                        val arriba = altoDisponible - altura

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(izquierda, arriba),
                            size = Size(anchoBarra, altura),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }

                datos.forEach { punto ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(punto.etiqueta, color = Color.Gray, fontSize = 12.sp)
                        Text(punto.valor.toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaMetrica(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(valor, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF0277BD))
            Text(etiqueta, color = Color.Gray, fontSize = 13.sp)
        }
    }
}
