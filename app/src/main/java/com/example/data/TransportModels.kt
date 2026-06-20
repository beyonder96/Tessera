package com.example.data

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
