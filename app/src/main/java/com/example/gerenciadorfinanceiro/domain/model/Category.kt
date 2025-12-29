package com.example.gerenciadorfinanceiro.domain.model

import androidx.compose.ui.graphics.Color

enum class CategoryType {
    INCOME,
    EXPENSE,
    BOTH
}

enum class Category(
    val displayName: String,
    val type: CategoryType,
    val color: Color
) {
    // Expenses
    FOOD("Alimentação", CategoryType.EXPENSE, Color(0xFFFF5722)),
    TRANSPORT("Transporte", CategoryType.EXPENSE, Color(0xFF2196F3)),
    HOUSING("Moradia", CategoryType.EXPENSE, Color(0xFF4CAF50)),
    HEALTH("Saúde", CategoryType.EXPENSE, Color(0xFFF44336)),
    EDUCATION("Educação", CategoryType.EXPENSE, Color(0xFF9C27B0)),
    ENTERTAINMENT("Lazer", CategoryType.EXPENSE, Color(0xFFFF9800)),
    SHOPPING("Compras", CategoryType.EXPENSE, Color(0xFFE91E63)),
    SERVICES("Serviços", CategoryType.EXPENSE, Color(0xFF607D8B)),
    SUBSCRIPTIONS("Assinaturas", CategoryType.EXPENSE, Color(0xFF795548)),
    BILLS("Contas", CategoryType.EXPENSE, Color(0xFF455A64)),
    PETS("Pets", CategoryType.EXPENSE, Color(0xFF8D6E63)),
    PERSONAL_CARE("Cuidados Pessoais", CategoryType.EXPENSE, Color(0xFFEC407A)),
    CLOTHING("Roupas", CategoryType.EXPENSE, Color(0xFFAB47BC)),
    TRAVEL("Viagem", CategoryType.EXPENSE, Color(0xFF29B6F6)),
    TAXES("Impostos", CategoryType.EXPENSE, Color(0xFFEF5350)),
    INSURANCE("Seguros", CategoryType.EXPENSE, Color(0xFF5C6BC0)),

    // Income
    SALARY("Salário", CategoryType.INCOME, Color(0xFF4CAF50)),
    FREELANCE("Freelance", CategoryType.INCOME, Color(0xFF2196F3)),
    INVESTMENTS("Investimentos", CategoryType.INCOME, Color(0xFFFF9800)),
    GIFTS("Presentes", CategoryType.INCOME, Color(0xFFE91E63)),
    BONUS("Bônus", CategoryType.INCOME, Color(0xFF66BB6A)),
    RENTAL("Aluguel Recebido", CategoryType.INCOME, Color(0xFF26A69A)),
    REFUND("Reembolso", CategoryType.INCOME, Color(0xFF42A5F5)),

    // Both
    OTHER("Outros", CategoryType.BOTH, Color(0xFF9E9E9E)),
    TRANSFER("Transferência", CategoryType.BOTH, Color(0xFF78909C));

    companion object {
        fun expenses(): List<Category> = entries.filter {
            it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH
        }

        fun incomes(): List<Category> = entries.filter {
            it.type == CategoryType.INCOME || it.type == CategoryType.BOTH
        }

        fun fromName(name: String): Category = entries.find { it.name == name } ?: OTHER
    }
}