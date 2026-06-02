package com.example.qhacemos.datos

import com.example.qhacemos.modelo.PerfilUsuario
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.StateFlow

private const val TABLA_PERFILES = "perfiles"

object GestorAutenticacion {

    private var sesionDemo: PerfilUsuario? = null

    val sessionStatus: StateFlow<SessionStatus>?
        get() = if (SupabaseCliente.estaConfigurado) {
            SupabaseCliente.cliente.auth.sessionStatus
        } else {
            null
        }

    fun usaModoDemo(): Boolean = !SupabaseCliente.estaConfigurado

    suspend fun iniciarSesion(
        email: String,
        password: String
    ): Result<PerfilUsuario> {
        val correo = email.trim()

        if (!SupabaseCliente.estaConfigurado) {
            val demo = GestorUsuarios.validarUsuarioDemo(correo, password)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Modo demo: usa admin@qhacemos.test o usuario@qhacemos.test"
                    )
                )
            if (demo.estaSuspendido) {
                return Result.failure(IllegalStateException("Tu cuenta esta suspendida. Contacta a un administrador."))
            }
            sesionDemo = demo
            return Result.success(demo)
        }

        return runCatching {
            SupabaseCliente.cliente.auth.signInWith(Email) {
                this.email = correo
                this.password = password
            }

            val perfil = cargarPerfilActual().getOrThrow()
                ?: throw IllegalStateException("La sesion se creo, pero no se encontro el perfil.")
            if (perfil.estaSuspendido) {
                SupabaseCliente.cliente.auth.signOut()
                throw IllegalStateException("Tu cuenta esta suspendida. Contacta a un administrador.")
            }
            perfil
        }
    }

    suspend fun cerrarSesion() {
        if (!SupabaseCliente.estaConfigurado) {
            sesionDemo = null
            return
        }
        SupabaseCliente.cliente.auth.signOut()
    }

    suspend fun cargarPerfilActual(): Result<PerfilUsuario?> {
        if (!SupabaseCliente.estaConfigurado) {
            return Result.success(sesionDemo)
        }

        return runCatching {
            val usuarioActual = SupabaseCliente.cliente.auth.currentUserOrNull()
                ?: SupabaseCliente.cliente.auth.currentSessionOrNull()?.user
                ?: return@runCatching null

            val perfiles = SupabaseCliente.cliente
                .from(TABLA_PERFILES)
                .select {
                    filter {
                        eq("id", usuarioActual.id)
                    }
                }
                .decodeList<PerfilUsuario>()

            perfiles.firstOrNull() ?: PerfilUsuario(
                id = usuarioActual.id,
                nombre = usuarioActual.email?.substringBefore("@").orEmpty(),
                email = usuarioActual.email.orEmpty(),
                rol = "usuario"
            )
        }
    }

}
