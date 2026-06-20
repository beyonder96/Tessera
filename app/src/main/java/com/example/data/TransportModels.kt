package com.example.data

import com.squareup.moshi.JsonClass

data class TransportTimeline(
    val tipoTransporte: String, // "onibus", "metro", "trem"
    val linhaIdentificador: String, // ex: "809P-10" ou "L2"
    val linhaNome: String, // ex: "Metrô Barra Funda / Terminal Pinheiros"
    val corTema: String, // Cor Hex (ex: "#12723a")
    val tempoRestanteTotalMin: Int,
    val statusLinha: String,
    val proximasParadas: List<TransportParada>
)

data class TransportParada(
    val paradaNome: String,
    val horarioPrevisto: String,
    val status: String, // "passou", "atual", "proxima", "destino"
    val mensagem: String? = null
)

@JsonClass(generateAdapter = true)
data class VehiclePosition(
    val prefixo: String,
    val latitude: Double,
    val longitude: Double,
    val horarioTransmissao: String? = null
)

@JsonClass(generateAdapter = true)
data class TimelineNode(
    val nome: String,
    val horarioPrevisto: String,
    val status: String, // "passou", "atual", "proxima", "destino"
    val mensagem: String? = null,
    val baldeacaoLinhasecores: List<String>? = null // Conexões de baldeação e cores (ex: "L1-Azul|#005CA9")
)

@JsonClass(generateAdapter = true)
data class RouteSession(
    val tipoModal: String, // "ONIBUS" ou "TRILHOS"
    val linhaCodigo: String,
    val corTema: String,
    val destinoFinal: String,
    val mapaVeiculos: List<VehiclePosition>? = null, // Preenchido se ONIBUS
    val passosTimeline: List<TimelineNode>? = null    // Preenchido se TRILHOS
)

