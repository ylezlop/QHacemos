package com.example.qhacemos.pantallas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qhacemos.datos.ResultadoEventos
import com.example.qhacemos.datos.cargarEventos
import com.example.qhacemos.modelo.Evento
import com.example.qhacemos.navigation.AppScreens
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class EventoBrujula(
    val evento: Evento,
    val distanciaMetros: Float,
    val direccionGrados: Float
)

@Composable
fun BrujulaEventosScreen(navController: NavController) {
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    var rumboDispositivo by remember { mutableFloatStateOf(0f) }
    var ubicacionActual by remember { mutableStateOf<Location?>(null) }
    var eventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var tienePermiso by remember { mutableStateOf(tienePermisoUbicacion(context)) }
    var indiceEvento by remember { mutableStateOf(0) }

    val permisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        tienePermiso = permisos.values.any { it }
        if (tienePermiso) {
            ubicacionActual = obtenerUltimaUbicacion(context)
        }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val acceleration = FloatArray(3)
        val magnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)

                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        rumboDispositivo = normalizarGrados(Math.toDegrees(orientation[0].toDouble()).toFloat())
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, acceleration, 0, acceleration.size)
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            rumboDispositivo = normalizarGrados(Math.toDegrees(orientation[0].toDouble()).toFloat())
                        }
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetic, 0, magnetic.size)
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, acceleration, magnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            rumboDispositivo = normalizarGrados(Math.toDegrees(orientation[0].toDouble()).toFloat())
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    DisposableEffect(context, tienePermiso) {
        if (!tienePermiso) {
            onDispose { }
        } else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = LocationListener { location ->
                ubicacionActual = location
            }

            registrarActualizacionesUbicacion(locationManager, listener)

            onDispose {
                locationManager.removeUpdates(listener)
            }
        }
    }

    LaunchedEffect(context, tienePermiso) {
        cargando = true
        mensaje = null

        if (!tienePermiso) {
            permisoLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            ubicacionActual = obtenerUltimaUbicacion(context)
        }

        when (val resultado = cargarEventos(context)) {
            is ResultadoEventos.Exito -> eventos = resultado.eventos.filter { it.esVisibleParaUsuarios() && it.tieneCoordenadas }
            is ResultadoEventos.Error -> {
                eventos = resultado.eventosLocales.filter { it.esVisibleParaUsuarios() && it.tieneCoordenadas }
                mensaje = resultado.mensaje
            }
        }

        cargando = false
    }

    val eventosOrdenados = remember(eventos, ubicacionActual) {
        val ubicacion = ubicacionActual ?: return@remember null
        eventos.mapNotNull { evento ->
            val latitud = evento.latitud ?: return@mapNotNull null
            val longitud = evento.longitud ?: return@mapNotNull null
            val resultado = FloatArray(3)
            Location.distanceBetween(
                ubicacion.latitude,
                ubicacion.longitude,
                latitud,
                longitud,
                resultado
            )
            EventoBrujula(evento, resultado[0], normalizarGrados(resultado[1]))
        }.sortedBy { it.distanciaMetros }
    }.orEmpty()

    LaunchedEffect(eventosOrdenados.size) {
        indiceEvento = when {
            eventosOrdenados.isEmpty() -> 0
            indiceEvento >= eventosOrdenados.size -> eventosOrdenados.lastIndex
            indiceEvento < 0 -> 0
            else -> indiceEvento
        }
    }

    val eventoObjetivo = eventosOrdenados.getOrNull(indiceEvento)

    val rotacionAguja = eventoObjetivo?.let {
        normalizarGrados(it.direccionGrados - rumboDispositivo)
    } ?: 0f

    Scaffold(
        bottomBar = {
            BarraNavegacionInferior(
                navController = navController,
                rutaActual = backStackEntry?.destination?.route
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F7FB))
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = eventoObjetivo?.evento?.titulo ?: "Brujula de eventos",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextoDistancia(
                eventoObjetivo = eventoObjetivo,
                tienePermiso = tienePermiso,
                eventosDisponibles = eventos.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(26.dp))

            Box(
                modifier = Modifier.size(270.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassCanvas(rotacionAguja = rotacionAguja)
                if (cargando) {
                    CircularProgressIndicator(color = Color(0xFF03A9F4))
                }
            }

            mensaje?.let {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = it,
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (!tienePermiso) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        permisoLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Permitir ubicacion")
                }
            }

            if (eventoObjetivo != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = indiceEvento > 0,
                        onClick = { indiceEvento-- },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0277BD),
                            disabledContainerColor = Color(0xFFE1E8EE),
                            disabledContentColor = Color(0xFF9EADB7)
                        )
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Evento anterior")
                    }

                    Button(
                        onClick = {
                            navController.navigate("${AppScreens.EventDetail.route}/${eventoObjetivo.evento.id}")
                        }
                    ) {
                        Text("Ver evento")
                    }

                    IconButton(
                        enabled = indiceEvento < eventosOrdenados.lastIndex,
                        onClick = { indiceEvento++ },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0277BD),
                            disabledContainerColor = Color(0xFFE1E8EE),
                            disabledContentColor = Color(0xFF9EADB7)
                        )
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente evento")
                    }
                }
            }
        }
    }
}

@Composable
private fun TextoDistancia(
    eventoObjetivo: EventoBrujula?,
    tienePermiso: Boolean,
    eventosDisponibles: Boolean
) {
    val texto = when {
        !tienePermiso -> "Permite ubicacion para calcular distancia"
        !eventosDisponibles -> "No hay eventos con coordenadas"
        eventoObjetivo == null -> "Esperando ubicacion del dispositivo"
        else -> "${formatearDistancia(eventoObjetivo.distanciaMetros)} de ti"
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFF03A9F4),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = texto,
            color = Color(0xFF0277BD),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompassCanvas(rotacionAguja: Float) {
    Canvas(modifier = Modifier.size(240.dp)) {
        val centro = center
        val radio = size.minDimension * 0.46f
        val radioInterno = radio * 0.78f

        drawCircle(Color.White, radius = radio, center = centro)
        drawCircle(Color(0xFFD6DFE6), radius = radio, center = centro, style = Stroke(width = 3.dp.toPx()))
        drawCircle(Color(0xFFEAF0F4), radius = radioInterno, center = centro, style = Stroke(width = 1.dp.toPx()))

        for (i in 0 until 360 step 10) {
            rotate(i.toFloat(), pivot = centro) {
                val largo = if (i % 30 == 0) 11.dp.toPx() else 6.dp.toPx()
                drawLine(
                    color = Color(0xFFB7C3CC),
                    start = Offset(centro.x, centro.y - radio),
                    end = Offset(centro.x, centro.y - radio + largo),
                    strokeWidth = if (i % 30 == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        val puntos = listOf(
            Triple("N", 0f, Color(0xFF263238)),
            Triple("E", 90f, Color(0xFF263238)),
            Triple("S", 180f, Color(0xFF263238)),
            Triple("W", 270f, Color(0xFF263238)),
            Triple("NE", 45f, Color(0xFF78909C)),
            Triple("SE", 135f, Color(0xFF78909C)),
            Triple("SW", 225f, Color(0xFF78909C)),
            Triple("NW", 315f, Color(0xFF78909C))
        )

        puntos.forEach { (texto, grados, color) ->
            val distancia = if (texto.length == 1) radio * 0.82f else radio * 0.67f
            val radianes = Math.toRadians((grados - 90).toDouble())
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = if (texto.length == 1) 17.sp.toPx() else 11.sp.toPx()
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                    this.color = color.toArgb()
                }
                drawText(
                    texto,
                    centro.x + cos(radianes).toFloat() * distancia,
                    centro.y + sin(radianes).toFloat() * distancia + paint.textSize / 3f,
                    paint
                )
            }
        }

        rotate(rotacionAguja, pivot = centro) {
            val puntaRoja = Path().apply {
                moveTo(centro.x, centro.y - radio * 0.82f)
                lineTo(centro.x - 13.dp.toPx(), centro.y + 4.dp.toPx())
                lineTo(centro.x, centro.y + 16.dp.toPx())
                lineTo(centro.x + 13.dp.toPx(), centro.y + 4.dp.toPx())
                close()
            }
            val puntaAzul = Path().apply {
                moveTo(centro.x, centro.y + radio * 0.82f)
                lineTo(centro.x - 12.dp.toPx(), centro.y - 2.dp.toPx())
                lineTo(centro.x, centro.y - 14.dp.toPx())
                lineTo(centro.x + 12.dp.toPx(), centro.y - 2.dp.toPx())
                close()
            }
            drawPath(puntaAzul, Color(0xFF60C6F2))
            drawPath(puntaRoja, Color(0xFFD84315))
        }

        drawCircle(Color(0xFF90A4AE), radius = 15.dp.toPx(), center = centro)
        drawCircle(Color.White, radius = 8.dp.toPx(), center = centro)
    }
}

private fun normalizarGrados(grados: Float): Float {
    return (grados + 360f) % 360f
}

private fun formatearDistancia(metros: Float): String {
    return if (metros >= 1000f) {
        "${String.format(java.util.Locale.US, "%.1f", metros / 1000f)} km"
    } else {
        "${metros.roundToInt()} m"
    }
}

private fun tienePermisoUbicacion(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun obtenerUltimaUbicacion(context: Context): Location? {
    if (!tienePermisoUbicacion(context)) return null

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.getProviders(true)
        .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
        .maxByOrNull { it.time }
}

@SuppressLint("MissingPermission")
private fun registrarActualizacionesUbicacion(
    locationManager: LocationManager,
    listener: LocationListener
) {
    locationManager.getProviders(true).forEach { provider ->
        locationManager.requestLocationUpdates(
            provider,
            2_000L,
            5f,
            listener
        )
    }
}
