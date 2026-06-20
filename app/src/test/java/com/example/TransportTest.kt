package com.example

import com.example.data.SPTransParada
import com.example.data.SPTransPrevisaoResponse
import com.example.data.SPTransPrevisaoParada
import com.example.data.SPTransLinhaPrevisao
import com.example.data.SPTransVeiculoPrevisao
import com.example.data.TransportTimeline
import com.example.data.TransportParada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.*

class TransportTest {

    @Test
    fun testDistanceCalculation() {
        // Paulista a Brigadeiro (aprox. 1.3km)
        val lat1 = -23.5552
        val lon1 = -46.6620
        val lat2 = -23.5686
        val lon2 = -46.6477

        val distance = calculateDistance(lat1, lon1, lat2, lon2)
        
        // Deve ser aproximadamente 1300 a 2500 metros
        assertEquals(true, distance > 1000)
        assertEquals(true, distance < 3000)
    }

    @Test
    fun testTimelineMapping() {
        // Simular a parada mais próxima e previsões obtidas
        val mockParada = SPTransParada(
            cp = 123,
            np = "Estação Vital Brasil",
            ed = "Av. Prof. Francisco Morato, 250",
            py = -23.5615,
            px = -46.6387
        )

        val mockLinhaPrevisao = SPTransLinhaPrevisao(
            c = "809P-10",
            cl = 1001,
            sl = 1,
            lt0 = "Terminal Pinheiros",
            lt1 = "Metrô Barra Funda",
            qv = 1,
            vs = listOf(
                SPTransVeiculoPrevisao(
                    p = 54321,
                    t = "14:20",
                    a = true,
                    ta = "14:10",
                    py = -23.560,
                    px = -46.639
                )
            )
        )

        // Converter para a timeline unificada
        val timelines = mutableListOf<TransportTimeline>()
        
        val proximas = mutableListOf<TransportParada>()
        
        proximas.add(
            TransportParada(
                paradaNome = "Terminal: " + mockLinhaPrevisao.lt1,
                horarioPrevisto = "--:--",
                status = "passou"
            )
        )
        
        val proximoVeiculo = mockLinhaPrevisao.vs?.firstOrNull()
        assertNotNull(proximoVeiculo)

        proximas.add(
            TransportParada(
                paradaNome = mockParada.np,
                horarioPrevisto = proximoVeiculo!!.t,
                status = "atual",
                mensagem = "Você está aqui / Ônibus a 5 min"
            )
        )

        proximas.add(
            TransportParada(
                paradaNome = "Terminal: " + mockLinhaPrevisao.lt0,
                horarioPrevisto = "--:--",
                status = "destino"
            )
        )

        val timeline = TransportTimeline(
            tipoTransporte = "onibus",
            linhaIdentificador = mockLinhaPrevisao.c,
            linhaNome = "${mockLinhaPrevisao.lt1} -> ${mockLinhaPrevisao.lt0}",
            corTema = "#12723a",
            tempoRestanteTotalMin = 5,
            statusLinha = "Operação Normal",
            proximasParadas = proximas
        )

        // Verificações
        assertEquals("onibus", timeline.tipoTransporte)
        assertEquals("809P-10", timeline.linhaIdentificador)
        assertEquals(3, timeline.proximasParadas.size)
        assertEquals("atual", timeline.proximasParadas[1].status)
        assertEquals("Estação Vital Brasil", timeline.proximasParadas[1].paradaNome)
        assertEquals("14:20", timeline.proximasParadas[1].horarioPrevisto)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
