package com.example.gerenciadorfinanceiro.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonConfig {
    fun createGson(): Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
}
