package com.example.gerenciadorfinanceiro.domain.model

data class ReleaseNote(
    val version: String,
    val features: List<String> = emptyList(),
    val fixes: List<String> = emptyList(),
    val improvements: List<String> = emptyList()
)

object ReleaseNotes {
    val notes = listOf(
        ReleaseNote(
            version = "1.7.0",
            features = listOf(
                "Sincronização de categoria em parcelas: alterar a categoria de uma parcela agora atualiza todas as parcelas associadas",
                "Seletor de data para itens do cartão: melhor gerenciamento das datas dos itens do cartão",
                "Lista de compras por data: compras agora são organizadas e divididas por data",
                "Notificações NuPay: suporte para parsing de notificações NuPay",
                "Faturas recolhíveis: faturas são exibidas de forma compacta com destaque para a próxima fatura aberta"
            ),
            fixes = listOf(
                "Recorrências semanais: corrigido cálculo de transações recorrentes semanais",
                "Pagamento de fatura: saldo da conta agora é deduzido corretamente ao pagar faturas",
                "Confirmação de ação: adicionado diálogo de confirmação para prevenir ações acidentais"
            )
        )
    )

    fun getLatestVersion(): String = notes.firstOrNull()?.version ?: ""

    fun getNoteForVersion(version: String): ReleaseNote? =
        notes.find { it.version == version }
}
