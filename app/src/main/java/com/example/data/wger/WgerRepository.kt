package com.example.data.wger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object WgerRepository {

    private val api by lazy { WgerApi.create() }

    private var cachedExercises: List<WgerExercise> = emptyList()

    val categoryFilters: List<MuscleCategoryFilter> = listOf(
        MuscleCategoryFilter(null, "Todos"),
        MuscleCategoryFilter(11, "Peito"),
        MuscleCategoryFilter(12, "Costas"),
        MuscleCategoryFilter(9, "Pernas"),
        MuscleCategoryFilter(13, "Ombros"),
        MuscleCategoryFilter(8, "Braços"),
        MuscleCategoryFilter(10, "Abdômen"),
        MuscleCategoryFilter(14, "Panturrilhas")
    )

    suspend fun getExercises(
        categoryId: Int? = null,
        searchQuery: String = "",
        forceRefresh: Boolean = false
    ): List<WgerExercise> = withContext(Dispatchers.IO) {
        if (cachedExercises.isEmpty() || forceRefresh) {
            try {
                val response = api.getExerciseInfo(language = 2, limit = 100)
                val mapped = response.results.map { it.toDomain() }
                if (mapped.isNotEmpty()) {
                    cachedExercises = mapped
                } else {
                    cachedExercises = getFallbackExercises()
                }
            } catch (e: Exception) {
                if (cachedExercises.isEmpty()) {
                    cachedExercises = getFallbackExercises()
                }
            }
        }

        var filtered = cachedExercises

        if (categoryId != null) {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            filtered = filtered.filter { ex ->
                ex.name.lowercase().contains(query) ||
                ex.categoryName.lowercase().contains(query) ||
                ex.primaryMuscles.any { it.lowercase().contains(query) } ||
                ex.equipment.any { it.lowercase().contains(query) }
            }
        }

        filtered
    }

    private fun WgerExerciseInfoDto.toDomain(): WgerExercise {
        val catId = category?.id ?: 0
        val catName = mapCategoryName(catId, category?.name ?: "Geral")
        val cleanDesc = cleanHtmlDescription(description)

        val mainImg = images.firstOrNull { it.isMain }?.image
            ?: images.firstOrNull()?.image

        return WgerExercise(
            id = id,
            name = translateCommonExerciseName(name),
            categoryId = catId,
            categoryName = catName,
            description = cleanDesc.ifBlank { "Exercício de musculação para desenvolvimento de força e hipertrofia." },
            primaryMuscles = muscles.map { it.name.ifBlank { it.nameEn } }.filter { it.isNotBlank() },
            secondaryMuscles = musclesSecondary.map { it.name.ifBlank { it.nameEn } }.filter { it.isNotBlank() },
            equipment = equipment.map { translateEquipment(it.name) },
            mainImageUrl = mainImg,
            allImageUrls = images.map { it.image }
        )
    }

    private fun mapCategoryName(id: Int, originalName: String): String {
        return when (id) {
            11 -> "Peito"
            12 -> "Costas"
            9 -> "Pernas"
            13 -> "Ombros"
            8 -> "Braços"
            10 -> "Abdômen"
            14 -> "Panturrilhas"
            15 -> "Cárdio"
            else -> originalName
        }
    }

    private fun translateEquipment(name: String): String {
        return when (name.lowercase()) {
            "barbell" -> "Barra"
            "dumbbell" -> "Halteres"
            "bench" -> "Banco Reto"
            "incline bench" -> "Banco Inclinado"
            "pull-up bar" -> "Barra Fixa"
            "bodyweight" -> "Peso Corporal"
            "kettlebell" -> "Kettlebell"
            "sz-bar" -> "Barra W"
            "gym mat" -> "Colchonete"
            "swiss ball" -> "Bola Suíça"
            else -> name
        }
    }

    private fun translateCommonExerciseName(name: String): String {
        return when {
            name.equals("Bench Press", ignoreCase = true) -> "Supino Reto com Barra"
            name.equals("Incline Bench Press", ignoreCase = true) -> "Supino Inclinado com Barra"
            name.equals("Squat", ignoreCase = true) -> "Agachamento Livre"
            name.equals("Deadlift", ignoreCase = true) -> "Levantamento Terra"
            name.equals("Overhead Press", ignoreCase = true) -> "Desenvolvimento Militar"
            name.equals("Bicep Curls", ignoreCase = true) -> "Rosca Direta com Barra"
            name.equals("Tricep Dips", ignoreCase = true) -> "Mergulho nas Paralelas"
            name.equals("Pull-ups", ignoreCase = true) -> "Barra Fixa (Pronada)"
            name.equals("Chin-ups", ignoreCase = true) -> "Barra Fixa (Supinada)"
            name.equals("Leg Press", ignoreCase = true) -> "Leg Press 45°"
            name.equals("Lateral Raises", ignoreCase = true) -> "Elevação Lateral"
            name.equals("Calf Raises", ignoreCase = true) -> "Elevação de Panturrilhas"
            name.equals("Plank", ignoreCase = true) -> "Prancha Abdominal"
            name.equals("Crunches", ignoreCase = true) -> "Abdominal Tradicional"
            name.equals("Dumbbell Flyes", ignoreCase = true) -> "Crucifixo com Halteres"
            name.equals("Bent Over Row", ignoreCase = true) -> "Remada Curvada com Barra"
            else -> name
        }
    }

    private fun cleanHtmlDescription(rawHtml: String): String {
        if (rawHtml.isBlank()) return ""
        return try {
            Jsoup.parse(rawHtml).text()
        } catch (_: Throwable) {
            rawHtml.replace(Regex("<.*?>"), " ").trim()
        }
    }

    /**
     * Catálogo local de fallback para garantir uso instantâneo e offline.
     */
    fun getFallbackExercises(): List<WgerExercise> {
        return listOf(
            WgerExercise(
                id = 101,
                name = "Supino Reto com Barra",
                categoryId = 11,
                categoryName = "Peito",
                description = "Deite-se no banco reto com os pés firmes no chão. Segure a barra com uma pegada um pouco mais larga que os ombros, desça controladamente até o meio do peitoral e empurre com força até a extensão quase completa dos braços.",
                primaryMuscles = listOf("Peitoral Maior"),
                secondaryMuscles = listOf("Tríceps", "Deltóide Anterior"),
                equipment = listOf("Barra", "Banco Reto"),
                mainImageUrl = "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 102,
                name = "Supino Inclinado com Halteres",
                categoryId = 11,
                categoryName = "Peito",
                description = "Ajuste o banco em uma inclinação de 30° a 45°. Segure os halteres na altura do peito superior e empurre verticalmente, focando na contração da porção clavicular do peitoral.",
                primaryMuscles = listOf("Peitoral Superior"),
                secondaryMuscles = listOf("Deltóide Anterior", "Tríceps"),
                equipment = listOf("Halteres", "Banco Inclinado"),
                mainImageUrl = "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 103,
                name = "Crucifixo na Polia / Crossover",
                categoryId = 11,
                categoryName = "Peito",
                description = "Posicione-se entre os cabos ajustados na altura média. Com os cotovelos levemente flexionados, traga as mãos à frente do corpo em um movimento circular até contrair totalmente o peitoral.",
                primaryMuscles = listOf("Peitoral Maior"),
                secondaryMuscles = listOf("Deltóide Anterior"),
                equipment = listOf("Cabos"),
                mainImageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 201,
                name = "Puxada Alta na Frente (Lat Pulldown)",
                categoryId = 12,
                categoryName = "Costas",
                description = "Sente-se no aparelho com as pernas presas no apoio. Segure a barra aberta com pegada pronada e puxe em direção ao topo do peitoral, projetando os cotovelos para baixo e para trás.",
                primaryMuscles = listOf("Latíssimo do Dorso"),
                secondaryMuscles = listOf("Bíceps", "Romboide"),
                equipment = listOf("Máquina / Cabo"),
                mainImageUrl = "https://images.unsplash.com/photo-1605296867304-46d5465a13f1?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 202,
                name = "Remada Curvada com Barra",
                categoryId = 12,
                categoryName = "Costas",
                description = "Incline o tronco para a frente mantendo a coluna alinhada e os joelhos levemente destravados. Puxe a barra em direção ao abdômen inferior contraindo as escápulas.",
                primaryMuscles = listOf("Dorsal", "Trapézio"),
                secondaryMuscles = listOf("Bíceps", "Lombar"),
                equipment = listOf("Barra"),
                mainImageUrl = "https://images.unsplash.com/photo-1534367507873-d2d7e24c797f?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 203,
                name = "Levantamento Terra Convencional",
                categoryId = 12,
                categoryName = "Costas",
                description = "Posicione os pés na largura do quadril sob a barra. Agarre a barra, mantenha as costas retas, ative o core e suba estendendo o quadril e joelhos simultaneamente.",
                primaryMuscles = listOf("Eretores da Espinha", "Glúteos"),
                secondaryMuscles = listOf("Posteriores de Coxa", "Trapézio", "Antebraço"),
                equipment = listOf("Barra"),
                mainImageUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 301,
                name = "Agachamento Livre com Barra",
                categoryId = 9,
                categoryName = "Pernas",
                description = "Apoie a barra sobre os trapézios. Mantenha os pés ligeiramente virados para fora e desça flexionando os joelhos e quadril até que as coxas fiquem pelo menos paralelas ao chão.",
                primaryMuscles = listOf("Quadríceps", "Glúteo Máximo"),
                secondaryMuscles = listOf("Posteriores de Coxa", "Core"),
                equipment = listOf("Barra"),
                mainImageUrl = "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 302,
                name = "Leg Press 45°",
                categoryId = 9,
                categoryName = "Pernas",
                description = "Posicione os pés na plataforma na largura dos ombros. Destrave a máquina e desça o peso de forma controlada até 90 graus de flexão de joelho, empurrando de volta sem travar as articulações.",
                primaryMuscles = listOf("Quadríceps"),
                secondaryMuscles = listOf("Glúteos"),
                equipment = listOf("Máquina"),
                mainImageUrl = "https://images.unsplash.com/photo-1434682881908-b43d0467b798?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 303,
                name = "Mesa Flexora",
                categoryId = 9,
                categoryName = "Pernas",
                description = "Deite-se de bruços com o rolo de apoio posicionado logo acima dos calcanhares. Flexione os joelhos trazendo os calcanhares em direção aos glúteos.",
                primaryMuscles = listOf("Isquiotibiais / Posteriores de Coxa"),
                secondaryMuscles = listOf("Gastrocnêmio"),
                equipment = listOf("Máquina"),
                mainImageUrl = "https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 401,
                name = "Desenvolvimento Militar com Barra",
                categoryId = 13,
                categoryName = "Ombros",
                description = "Em pé ou sentado, empurre a barra a partir da altura da clavícula até a extensão acima da cabeça, mantendo o abdômen contraído para proteger a lombar.",
                primaryMuscles = listOf("Deltóide Anterior e Médio"),
                secondaryMuscles = listOf("Tríceps", "Trapézio"),
                equipment = listOf("Barra"),
                mainImageUrl = "https://images.unsplash.com/photo-1541534741688-6078c6bfb5c5?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 402,
                name = "Elevação Lateral com Halteres",
                categoryId = 13,
                categoryName = "Ombros",
                description = "Em pé, eleve os halteres lateralmente com os cotovelos levemente flexionados até a altura dos ombros, focando no isolamento da porção lateral do deltóide.",
                primaryMuscles = listOf("Deltóide Lateral"),
                secondaryMuscles = listOf("Trapézio Superior"),
                equipment = listOf("Halteres"),
                mainImageUrl = "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 501,
                name = "Rosca Direta com Barra W",
                categoryId = 8,
                categoryName = "Braços",
                description = "Segure a barra W com pegada supinada. Mantenha os cotovelos fixos junto ao tronco e flexione os antebraços até a contração máxima do bíceps.",
                primaryMuscles = listOf("Bíceps Braquial"),
                secondaryMuscles = listOf("Braquial", "Antebraço"),
                equipment = listOf("Barra W"),
                mainImageUrl = "https://images.unsplash.com/photo-1581009146145-b5ef050c2e1e?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 502,
                name = "Tríceps Corda no Pulley",
                categoryId = 8,
                categoryName = "Braços",
                description = "Prenda a corda na polia alta. Com os cotovelos colados ao corpo, estenda os braços para baixo abrindo a corda no final do movimento para contração total.",
                primaryMuscles = listOf("Tríceps"),
                secondaryMuscles = listOf("Antebraço"),
                equipment = listOf("Cabos"),
                mainImageUrl = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 601,
                name = "Prancha Abdominal Isométrica",
                categoryId = 10,
                categoryName = "Abdômen",
                description = "Apoie os antebraços e as pontas dos pés no chão. Mantenha o corpo em linha reta da cabeça aos calcanhares, contraindo intensamente o abdômen e glúteos.",
                primaryMuscles = listOf("Reto Abdominal", "Transverso do Abdômen"),
                secondaryMuscles = listOf("Lombar", "Ombros"),
                equipment = listOf("Colchonete"),
                mainImageUrl = "https://images.unsplash.com/photo-1566241142559-40e1dab266c6?w=400&h=300&fit=crop"
            ),
            WgerExercise(
                id = 701,
                name = "Elevação de Panturrilha em Pé",
                categoryId = 14,
                categoryName = "Panturrilhas",
                description = "Apoie a ponta dos pés em um degrau ou plataforma. Desça os calcanhares para alongar a panturrilha e eleve o corpo o máximo possível na ponta dos pés.",
                primaryMuscles = listOf("Gastrocnêmio", "Sóleo"),
                secondaryMuscles = listOf("Tibial"),
                equipment = listOf("Máquina / Degrau"),
                mainImageUrl = "https://images.unsplash.com/photo-1574680096145-d05b474e2155?w=400&h=300&fit=crop"
            )
        )
    }
}
