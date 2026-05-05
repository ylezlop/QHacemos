package com.example.qhacemos.datos

import com.example.qhacemos.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseCliente {
    val estaConfigurado: Boolean
        get() = BuildConfig.SUPABASE_URL.startsWith("https://") &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank() &&
            !BuildConfig.SUPABASE_URL.contains("TU-PROYECTO", ignoreCase = true) &&
            !BuildConfig.SUPABASE_ANON_KEY.contains("TU_ANON", ignoreCase = true)

    val cliente by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
