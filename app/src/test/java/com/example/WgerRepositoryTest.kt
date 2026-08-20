package com.example

import com.example.data.wger.WgerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WgerRepositoryTest {

    @Test
    fun testFallbackExercisesCatalog() {
        val fallback = WgerRepository.getFallbackExercises()
        assertTrue(fallback.isNotEmpty())
        assertTrue(fallback.size >= 10)

        // Supino
        val benchPress = fallback.find { it.name.contains("Supino Reto", ignoreCase = true) }
        assertNotNull(benchPress)
        assertEquals("Peito", benchPress?.categoryName)
        assertEquals(11, benchPress?.categoryId)
        assertTrue(benchPress?.equipment?.contains("Barra") == true)

        // Agachamento
        val squat = fallback.find { it.name.contains("Agachamento", ignoreCase = true) }
        assertNotNull(squat)
        assertEquals("Pernas", squat?.categoryName)
        assertEquals(9, squat?.categoryId)
    }

    @Test
    fun testCategoryFiltersList() {
        val filters = WgerRepository.categoryFilters
        assertTrue(filters.isNotEmpty())
        assertEquals("Todos", filters.first().displayName)
        assertNull(filters.first().id)

        val chestFilter = filters.find { it.displayName == "Peito" }
        assertNotNull(chestFilter)
        assertEquals(11, chestFilter?.id)
    }

    @Test
    fun testFilterByCategoryAndSearch() = runBlocking {
        // Filtrando Peito
        val chestExercises = WgerRepository.getExercises(categoryId = 11)
        assertTrue(chestExercises.isNotEmpty())
        assertTrue(chestExercises.all { it.categoryId == 11 })

        // Filtrando busca por texto "agachamento"
        val squatSearch = WgerRepository.getExercises(searchQuery = "agachamento")
        assertTrue(squatSearch.isNotEmpty())
        assertTrue(squatSearch.any { it.name.contains("Agachamento", ignoreCase = true) })
    }
}
