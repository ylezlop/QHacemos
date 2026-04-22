    package com.example.qhacemos.pantallas
    
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.LazyRow
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import com.example.qhacemos.datos.leerEventos
    import com.example.qhacemos.modelo.Evento
    import com.example.qhacemos.ui.theme.QHacemosTheme
    import androidx.navigation.NavController
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PantallaPrincipal(navController: NavController) {
        val contexto = LocalContext.current
        val listaEventos = remember { leerEventos(contexto) }

        Scaffold(
            bottomBar = { BarraNavegacionInferior() }
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
                item { SeccionBarraBusqueda() }
                item { SeccionCategorias() }


                item {
                    val destacados = listaEventos.filter { it.esDestacado }
                    if (destacados.isNotEmpty()) {
                        SeccionEventosDestacados(eventos = destacados)
                    }
                }

                item {
                    Text(
                        text = "Próximos eventos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val proximos = listaEventos.filter { !it.esDestacado }


                if (proximos.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "¡Aún no hay eventos próximos en esta área!",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
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
    
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun SeccionBarraBusqueda() {
            OutlinedTextField(
                value = "",
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Buscar talleres, conciertos, ferias...",
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
                            .clickable { }
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
        fun SeccionCategorias() {
            val categorias = listOf("Todos", "Talleres", "Conciertos", "Teatro", "Ferias")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categorias) { categoria ->
                    val estaSeleccionada = categoria == "Todos"
                    Surface(
                        modifier = Modifier.clickable { },
                        shape = RoundedCornerShape(20.dp),
                        color = if (estaSeleccionada) Color(0xFF4FC3F7) else Color.White,
                        border = if (!estaSeleccionada) androidx.compose.foundation.BorderStroke(
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
        fun SeccionEventosDestacados(eventos: List<Evento>) {
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
                    items(eventos) { evento -> TarjetaEventoDestacado(evento = evento) }
                }
            }
        }
    
        @Composable
        fun TarjetaEventoDestacado(evento: Evento) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.width(200.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(evento.colorFondo)
                    )
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
                    .clickable { onClick() } //
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(evento.colorFondo)
                    ) {
                        if (evento.esGratis) {
                            Surface(
                                color = Color(0xFF00BFA5),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Gratis",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
    
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = evento.titulo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
    
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
                                text = evento.fechaHora, //
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
        fun BarraNavegacionInferior() {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = true,
                    onClick = { })
                NavigationBarItem(icon = {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Mapa"
                    )
                }, label = { Text("Mapa") }, selected = false, onClick = { })
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
                    label = { Text("Crear") }, selected = false, onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Cuenta") },
                    label = { Text("Cuenta") },
                    selected = false,
                    onClick = { })
            }
        }

    
