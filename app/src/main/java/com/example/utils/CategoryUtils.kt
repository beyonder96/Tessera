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

    /**
     * Tenta sugerir uma categoria baseando-se no título e/ou subtítulo do lançamento bancário.
     * Retorna o nome da categoria ou null se não houver correspondência clara.
     */
    fun suggestCategory(title: String, subtitle: String = ""): String? {
        val fullText = unaccent("$title $subtitle").trim().lowercase(Locale.getDefault())
        if (fullText.isBlank()) return null

        return when {
            // Alimentação
            fullText.contains("ifood") || fullText.contains("rappi") || fullText.contains("mcdonald") ||
                    fullText.contains("burger king") || fullText.contains("habib") || fullText.contains("subway") ||
                    fullText.contains("starbucks") || fullText.contains("outback") || fullText.contains("coco bambu") ||
                    fullText.contains("madero") || fullText.contains("restaurante") || fullText.contains("churrascaria") ||
                    fullText.contains("pizzaria") || fullText.contains("sushi") || fullText.contains("burger") ||
                    fullText.contains("padaria") || fullText.contains("panificadora") || fullText.contains("cafe") ||
                    fullText.contains("cafeteria") || fullText.contains("supermercado") || fullText.contains("mercado") ||
                    fullText.contains("carrefour") || fullText.contains("pao de acucar") || fullText.contains("assai") ||
                    fullText.contains("atacadao") || fullText.contains("sams club") || fullText.contains("dia%") ||
                    fullText.contains("extra") || fullText.contains("st marche") || fullText.contains("sonda") ||
                    fullText.contains("hortifruti") || fullText.contains("sacolao") || fullText.contains("acougue") ||
                    fullText.contains("confeitaria") || fullText.contains("doceria") || fullText.contains("sorveteria") ||
                    fullText.contains("lanchonete") || fullText.contains("cacau show") || fullText.contains("bauducco") -> "Alimentação"

            // Transporte
            fullText.contains("uber") || fullText.contains("99app") || fullText.contains("99 app") ||
                    fullText.contains("99pop") || fullText.contains("99 taxi") || fullText.contains("cabify") ||
                    fullText.contains("indrive") || fullText.contains("posto") || fullText.contains("shell") ||
                    fullText.contains("ipiranga") || fullText.contains("petrobras") || fullText.contains("br mania") ||
                    fullText.contains("combustivel") || fullText.contains("gasolina") || fullText.contains("etanol") ||
                    fullText.contains("estacionamento") || fullText.contains("estapar") || fullText.contains("indigo") ||
                    fullText.contains("sem parar") || fullText.contains("veloe") || fullText.contains("taggy") ||
                    fullText.contains("conectcar") || fullText.contains("pedagio") || fullText.contains("bilhete unico") ||
                    fullText.contains("sptrans") || fullText.contains("metro") || fullText.contains("cptm") ||
                    fullText.contains("autopass") || fullText.contains("top sp") || fullText.contains("latam") ||
                    fullText.contains("gol linhas") || fullText.contains("azul linhas") || fullText.contains("buser") ||
                    fullText.contains("clickbus") || fullText.contains("auto pecas") || fullText.contains("mecanica") ||
                    fullText.contains("oficina") || fullText.contains("borracharia") || fullText.contains("lava rapido") ||
                    fullText.contains("ipva") || fullText.contains("detran") -> "Transporte"

            // Saúde
            fullText.contains("drogasil") || fullText.contains("droga raia") || fullText.contains("raiadrogasil") ||
                    fullText.contains("pague menos") || fullText.contains("panvel") || fullText.contains("dpsp") ||
                    fullText.contains("drogaria") || fullText.contains("farmacia") || fullText.contains("medicamento") ||
                    fullText.contains("laboratorio") || fullText.contains("fleury") || fullText.contains("delboni") ||
                    fullText.contains("lavoisier") || fullText.contains("pardini") || fullText.contains("consulta") ||
                    fullText.contains("medico") || fullText.contains("clinica") || fullText.contains("hospital") ||
                    fullText.contains("pronto socorro") || fullText.contains("unimed") || fullText.contains("amil") ||
                    fullText.contains("bradesco saude") || fullText.contains("sulamerica") || fullText.contains("notredame") ||
                    fullText.contains("odontoprev") || fullText.contains("dentista") || fullText.contains("odonto") ||
                    fullText.contains("psicolog") || fullText.contains("terapia") || fullText.contains("fisioterapia") ||
                    fullText.contains("nutricion") || fullText.contains("otica") -> "Saúde"

            // Moradia
            fullText.contains("enel") || fullText.contains("light ") || fullText.contains("copel") ||
                    fullText.contains("cemig") || fullText.contains("celesc") || fullText.contains("equatorial") ||
                    fullText.contains("sabesp") || fullText.contains("copasa") || fullText.contains("corsan") ||
                    fullText.contains("sanepar") || fullText.contains("comgas") || fullText.contains("ultragaz") ||
                    fullText.contains("luz ") || fullText.contains("energia") || fullText.contains("agua ") ||
                    fullText.contains("saneamento") || fullText.contains("gas ") || fullText.contains("aluguel") ||
                    fullText.contains("condominio") || fullText.contains("quintoandar") || fullText.contains("lello") ||
                    fullText.contains("claro") || fullText.contains("vivo ") || fullText.contains("tim ") ||
                    fullText.contains("oi ") || fullText.contains("internet") || fullText.contains("banda larga") ||
                    fullText.contains("iptu") || fullText.contains("diarista") -> "Moradia"

            // Lazer
            fullText.contains("netflix") || fullText.contains("spotify") || fullText.contains("amazon prime") ||
                    fullText.contains("prime video") || fullText.contains("disney") || fullText.contains("hbo") ||
                    fullText.contains("star+") || fullText.contains("youtube premium") || fullText.contains("deezer") ||
                    fullText.contains("apple music") || fullText.contains("cinema") || fullText.contains("cinemark") ||
                    fullText.contains("cinepolis") || fullText.contains("ingresso.com") || fullText.contains("teatro") ||
                    fullText.contains("sympla") || fullText.contains("eventim") || fullText.contains("steam") ||
                    fullText.contains("playstation") || fullText.contains("psn") || fullText.contains("xbox") ||
                    fullText.contains("nintendo") || fullText.contains("riot games") || fullText.contains("blizzard") ||
                    fullText.contains("epic games") || fullText.contains("airbnb") || fullText.contains("booking") ||
                    fullText.contains("decolar") || fullText.contains("hotel") || fullText.contains("parque") -> "Lazer"

            // Educação
            fullText.contains("udemy") || fullText.contains("coursera") || fullText.contains("alura") ||
                    fullText.contains("domestika") || fullText.contains("rocketseat") || fullText.contains("faculdade") ||
                    fullText.contains("universidade") || fullText.contains("puc") || fullText.contains("estacio") ||
                    fullText.contains("fiap") || fullText.contains("colegio") || fullText.contains("escola") ||
                    fullText.contains("idiomas") || fullText.contains("wizard") || fullText.contains("cna") ||
                    fullText.contains("fisk") || fullText.contains("livraria") || fullText.contains("kindle") ||
                    fullText.contains("mensalidade escolar") -> "Educação"

            // Petz
            fullText.contains("petz") || fullText.contains("cobasi") || fullText.contains("petlove") ||
                    fullText.contains("veterinario") || fullText.contains("racao") || fullText.contains("banho e tosa") ||
                    fullText.contains("pet shop") || fullText.contains("petshop") -> "Petz"

            // Salário
            fullText.contains("folha de pagamento") || fullText.contains("salario") || fullText.contains("vencimento") ||
                    fullText.contains("remuneracao") || fullText.contains("pro labore") || fullText.contains("pro-labore") ||
                    fullText.contains("proventos") || fullText.contains("rendimento salarial") || fullText.contains("adiantamento salarial") ||
                    fullText.contains("plr") || fullText.contains("decimo terceiro") || fullText.contains("13o salario") -> "Salário"

            // Investimentos
            fullText.contains("nu invest") || fullText.contains("nuinvest") || fullText.contains("xp invest") ||
                    fullText.contains("rico corretora") || fullText.contains("btg pactual") || fullText.contains("inter dtvm") ||
                    fullText.contains("clear corretora") || fullText.contains("tesouro direto") || fullText.contains("cdi") ||
                    fullText.contains("dividendo") || fullText.contains("juros sobre capital") || fullText.contains("resgate cdb") ||
                    fullText.contains("rendimento poupanca") || fullText.contains("aplicacao financeira") -> "Investimentos"

            // Compras
            fullText.contains("amazon") || fullText.contains("mercado livre") || fullText.contains("mercadolivre") ||
                    fullText.contains("shopee") || fullText.contains("shein") || fullText.contains("aliexpress") ||
                    fullText.contains("magalu") || fullText.contains("magazine luiza") || fullText.contains("americanas") ||
                    fullText.contains("casas bahia") || fullText.contains("ponto frio") || fullText.contains("leroy merlin") ||
                    fullText.contains("telhanorte") || fullText.contains("c&a") || fullText.contains("renner") ||
                    fullText.contains("riachuelo") || fullText.contains("zara") || fullText.contains("decathlon") ||
                    fullText.contains("centauro") || fullText.contains("nike") || fullText.contains("adidas") ||
                    fullText.contains("kalunga") || fullText.contains("fast shop") || fullText.contains("kabum") ||
                    fullText.contains("pichau") || fullText.contains("terabyte") -> "Compras"

            // Reembolso
            fullText.contains("reembolso") || fullText.contains("estorno") || fullText.contains("devolucao") ||
                    fullText.contains("cashback") || fullText.contains("chargeback") -> "Reembolso"

            // Transferência
            fullText.contains("transferencia") || fullText.contains("ted ") || fullText.contains("doc ") ||
                    fullText.contains("tef ") || fullText.contains("pix ") -> "Transferência"

            else -> null
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
