package com.example.gerenciadorfinanceiro.domain.model

enum class NotificationSource(val displayName: String, val packageName: String) {
    ITAU("Itaú", "com.itau"),
    NUBANK("Nubank", "com.nu.production"),
    GOOGLE_WALLET("Google Wallet", "com.google.android.apps.walletnfcrel");

    companion object {
        fun fromPackageName(packageName: String): NotificationSource? = when {
            entries.any { it.packageName == packageName } ->
                entries.first { it.packageName == packageName }
            // Itaú has separate official applications (for example, its iti wallet).
            // Android package names are unique, so accepting this official namespace lets
            // transaction notifications keep working when they come from one of them.
            packageName.startsWith("com.itau.") -> ITAU
            else -> null
        }
    }
}
