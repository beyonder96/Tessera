package com.example.utils

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val isCustom: Boolean = false
)

object CategoryUtils {

    val DEFAULT_CATEGORIES: List<CategoryItem> = listOf(
        CategoryItem("Alimentação", Icons.Outlined.Restaurant),
        CategoryItem("Transporte", Icons.Outlined.DirectionsCar),
        CategoryItem("Moradia", Icons.Outlined.Home),
        CategoryItem("Saúde", Icons.Outlined.MedicalServices),
        CategoryItem("Lazer", Icons.Outlined.Movie),
        CategoryItem("Educação", Icons.Outlined.School),
        CategoryItem("Petz", Icons.Outlined.Pets),
        CategoryItem("Salário", Icons.Outlined.AttachMoney),
        CategoryItem("Investimentos", Icons.Outlined.TrendingUp),
        CategoryItem("Compras", Icons.Outlined.ShoppingBag),
        CategoryItem("Reembolso", Icons.Outlined.Undo),
        CategoryItem("Outros", Icons.Outlined.Receipt),
        CategoryItem("Transferência", Icons.Outlined.SwapHoriz)
    )

    fun getCategoryIcon(category: String?): ImageVector {
        if (category.isNullOrBlank()) return Icons.Outlined.Receipt
        val clean = unaccent(category.trim()).lowercase()

        return when {
            clean.contains("aliment") || clean.contains("comida") || clean.contains("mercado") ||
                    clean.contains("restaurante") || clean.contains("padaria") || clean.contains("ifood") -> Icons.Outlined.Restaurant

            clean.contains("transp") || clean.contains("uber") || clean.contains("carro") ||
                    clean.contains("combustivel") || clean.contains("posto") || clean.contains("metro") ||
                    clean.contains("onibus") -> Icons.Outlined.DirectionsCar

            clean.contains("moradia") || clean.contains("casa") || clean.contains("aluguel") ||
                    clean.contains("condominio") || clean.contains("luz") || clean.contains("agua") ||
                    clean.contains("energia") || clean.contains("gas") || clean.contains("internet") -> Icons.Outlined.Home

            clean.contains("saude") || clean.contains("farmacia") || clean.contains("drogaria") ||
                    clean.contains("medico") || clean.contains("hospital") || clean.contains("consulta") -> Icons.Outlined.MedicalServices

            clean.contains("lazer") || clean.contains("entretenimento") || clean.contains("cinema") ||
                    clean.contains("streaming") || clean.contains("netflix") || clean.contains("spotify") ||
                    clean.contains("jogo") || clean.contains("viagem") -> Icons.Outlined.Movie

            clean.contains("educa") || clean.contains("curso") || clean.contains("escola") ||
                    clean.contains("faculdade") || clean.contains("livro") -> Icons.Outlined.School

            clean.contains("pet") || clean.contains("veterinario") || clean.contains("racao") -> Icons.Outlined.Pets

            clean.contains("salario") || clean.contains("remuneracao") || clean.contains("renda") -> Icons.Outlined.AttachMoney

            clean.contains("invest") || clean.contains("cdi") || clean.contains("dividendo") ||
                    clean.contains("poupanca") -> Icons.Outlined.TrendingUp

            clean.contains("compra") || clean.contains("shopping") || clean.contains("roupa") ||
                    clean.contains("vestuario") -> Icons.Outlined.ShoppingBag

            clean.contains("reembolso") || clean.contains("estorno") || clean.contains("devolucao") -> Icons.Outlined.Undo

            clean.contains("transf") || clean.contains("ted") || clean.contains("doc") -> Icons.Outlined.SwapHoriz

            else -> Icons.Outlined.Receipt
        }
    }

    fun getAllCategories(context: Context): List<CategoryItem> {
        val sharedPrefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
        val customSet = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()

        val customItems = customSet.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val name = parts[0].trim()
                val icon = if (parts.size >= 2) {
                    when (parts[1]) {
                        "Home" -> Icons.Outlined.Home
                        "Pets" -> Icons.Outlined.Pets
                        "DirectionsCar" -> Icons.Outlined.DirectionsCar
                        "ShoppingBag" -> Icons.Outlined.ShoppingBag
                        "Flight" -> Icons.Outlined.Flight
                        "School" -> Icons.Outlined.School
                        "FitnessCenter" -> Icons.Outlined.FitnessCenter
                        "LocalDining" -> Icons.Outlined.LocalDining
                        else -> Icons.Outlined.Label
                    }
                } else {
                    getCategoryIcon(name)
                }
                CategoryItem(name = name, icon = icon, isCustom = true)
            } else null
        }.sortedBy { it.name }

        val defaultNames = DEFAULT_CATEGORIES.map { it.name.lowercase() }.toSet()
        val uniqueCustom = customItems.filterNot { defaultNames.contains(it.name.lowercase()) }

        return DEFAULT_CATEGORIES + uniqueCustom
    }

    private fun unaccent(text: String): String {
        val temp = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return temp.replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "").lowercase(Locale.getDefault())
    }
}
