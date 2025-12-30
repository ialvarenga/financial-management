package com.example.gerenciadorfinanceiro.domain.model

enum class NotificationSource(val displayName: String, val packageName: String) {
    ITAU("Itaú", "com.itau"),
    NUBANK("Nubank", "com.nu.production"),
    GOOGLE_WALLET("Google Wallet", "com.google.android.apps.walletnfcrel");

    companion object {
        fun fromPackageName(packageName: String): NotificationSource? =
            entries.find { it.packageName == packageName }
    }
}
