package com.example

import com.example.data.MetroCptmApi
import com.example.data.getMetroLineColor
import org.junit.Assert.*
import org.junit.Test

class MetroCptmApiTest {

    @Test
    fun testDefaultEmpresasStructure() {
        val empresas = MetroCptmApi.defaultEmpresas
        assertEquals(3, empresas.size)

        val totalLines = empresas.sumOf { it.linhas?.size ?: 0 }
        assertEquals(13, totalLines)

        // Metrô SP: Linhas 1, 2, 3, 15
        val metroSp = empresas.find { it.id == 1 }
        assertNotNull(metroSp)
        assertEquals(4, metroSp?.linhas?.size)
        assertTrue(metroSp?.linhas?.any { it.codigo == "1" && it.nome == "Azul" } == true)
        assertTrue(metroSp?.linhas?.any { it.codigo == "2" && it.nome == "Verde" } == true)
        assertTrue(metroSp?.linhas?.any { it.codigo == "3" && it.nome == "Vermelha" } == true)
        assertTrue(metroSp?.linhas?.any { it.codigo == "15" && it.nome == "Prata" } == true)

        // Concessões ViaQuatro / ViaMobilidade: Linhas 4, 5, 8, 9
        val viaQuatroMobilidade = empresas.find { it.id == 2 }
        assertNotNull(viaQuatroMobilidade)
        assertEquals(4, viaQuatroMobilidade?.linhas?.size)
        assertTrue(viaQuatroMobilidade?.linhas?.any { it.codigo == "4" && it.nome == "Amarela" } == true)
        assertTrue(viaQuatroMobilidade?.linhas?.any { it.codigo == "5" && it.nome == "Lilás" } == true)
        assertTrue(viaQuatroMobilidade?.linhas?.any { it.codigo == "8" && it.nome == "Diamante" } == true)
        assertTrue(viaQuatroMobilidade?.linhas?.any { it.codigo == "9" && it.nome == "Esmeralda" } == true)

        // CPTM / TIC Trens: Linhas 7, 10, 11, 12, 13
        val cptm = empresas.find { it.id == 3 }
        assertNotNull(cptm)
        assertEquals(5, cptm?.linhas?.size)
        assertTrue(cptm?.linhas?.any { it.codigo == "7" && it.nome == "Rubi" } == true)
        assertTrue(cptm?.linhas?.any { it.codigo == "10" && it.nome == "Turquesa" } == true)
        assertTrue(cptm?.linhas?.any { it.codigo == "11" && it.nome == "Coral" } == true)
        assertTrue(cptm?.linhas?.any { it.codigo == "12" && it.nome == "Safira" } == true)
        assertTrue(cptm?.linhas?.any { it.codigo == "13" && it.nome == "Jade" } == true)
    }

    @Test
    fun testMetroLineColors() {
        val blueColor = getMetroLineColor("Azul", "1")
        val yellowColor = getMetroLineColor("Amarela", "4")
        val rubyColor = getMetroLineColor("Rubi", "7")

        assertNotEquals(blueColor, yellowColor)
        assertNotEquals(yellowColor, rubyColor)
    }
}
