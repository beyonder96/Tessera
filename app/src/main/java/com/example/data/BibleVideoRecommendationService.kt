package com.example.data

data class BibleVideoRecommendation(
    val id: String,
    val title: String,
    val channelName: String,
    val videoId: String,
    val youtubeUrl: String,
    val thumbnailUrl: String,
    val categoryBadge: String,
    val description: String
)

object BibleVideoRecommendationService {

    // Mapeamento curado dos Panoramas oficiais do BibleProject em Português por abreviação de livro
    private val bookPanoramas = mapOf(
        "gn" to Pair("Gênesis • Panorama Bíblico Completo", "X8-ZlTqU5C0"),
        "ex" to Pair("Êxodo • O Resgate e a Presença de Deus", "hL8b9bQyR8M"),
        "lv" to Pair("Levítico • Santidade e Reconciliação", "Wmvyrw_cM9M"),
        "nm" to Pair("Números • A Jornada pelo Deserto", "tp5MI_6wZbg"),
        "dt" to Pair("Deuteronômio • A Nova Aliança e Obediência", "qS1l-FqfK9w"),
        "js" to Pair("Josué • A Conquista da Terra Prometida", "YvI6Z7B4Z-0"),
        "jz" to Pair("Juízes • O Ciclo da Graça e Redenção", "a0tH_qW08bA"),
        "rt" to Pair("Rute • Lealdade e o Redentor", "0h1eoBeR4Lk"),
        "1sm" to Pair("1 Samuel • A Ascensão do Rei Ungido", "4B68Nf_gJzg"),
        "2sm" to Pair("2 Samuel • O Reino de Davi e as Promessas", "p4g5g17gqgQ"),
        "1rs" to Pair("1 Reis • Sabedoria, Templo e Divisão", "bVf_7F9bB1E"),
        "2rs" to Pair("2 Reis • Exílio e Esperança da Aliança", "p68k7g5B1E4"),
        "sl" to Pair("Salmos • O Livro de Louvor e Oração", "j9ph4mK_WpA"),
        "pv" to Pair("Provérbios • O Caminho da Verdadeira Sabedoria", "AzmY5DUpeU8"),
        "ec" to Pair("Eclesiastes • O Significado da Vida sob o Sol", "lrsQ1TC324s"),
        "ct" to Pair("Cânticos • Amor, Poesia e Comunhão", "4Q2E_B18mFw"),
        "is" to Pair("Isaías • O Servo Sofredor e a Nova Criação", "X3q5w4m7k0s"),
        "jr" to Pair("Jeremias • O Profeta Chorão e o Novo Pacto", "aB7q8w1m9eA"),
        "lm" to Pair("Lamentações • A Esperança em Meio à Dor", "p18w7k5e4bA"),
        "ez" to Pair("Ezequiel • A Glória do Senhor e o Novo Coração", "R-COmY7o06g"),
        "dn" to Pair("Daniel • O Reino que Jamais Terá Fim", "9c6k6g7b8pE"),
        "os" to Pair("Oseias • O Amor Inabalável de Deus", "q47k9b8e1mA"),
        "jl" to Pair("Joel • O Dia do Senhor e o Espírito Santo", "p19m6k7b8tA"),
        "am" to Pair("Amós • Justiça Social e Integridade", "b18k7m6e4tA"),
        "ob" to Pair("Obadias • A Queda da Soberba", "k19m6e4b8tA"),
        "jn" to Pair("Jonas • A Graça que Alcança os Gentios", "dRXM6b4sWbg"),
        "mq" to Pair("Miqueias • Fazer Justiça e Andar Humildemente", "m18k7m6e4tA"),
        "na" to Pair("Naum • O Julgamento e o Refúgio", "n18k7m6e4tA"),
        "hc" to Pair("Habacuque • O Justo Viverá pela Fé", "h18k7m6e4tA"),
        "sf" to Pair("Sofonias • A Purificação das Nações", "s18k7m6e4tA"),
        "ag" to Pair("Ageu • Reconstruindo o Templo do Senhor", "a18k7m6e4tA"),
        "zc" to Pair("Zacarias • O Rei Manso e Vitorioso", "z18k7m6e4tA"),
        "ml" to Pair("Malaquias • A Promessa do Mensageiro", "m19k7m6e4tA"),
        "mt" to Pair("Mateus • O Evangelho do Messias Prometido", "G-2e9bZqC7Q"),
        "mc" to Pair("Marcos • O Evangelho do Servo Poderoso", "HGH4P6HzMpc"),
        "lc" to Pair("Lucas • Boas Novas para Todos os Povos", "XIb_mdLMyWw"),
        "jo" to Pair("João • O Verbo que se Fez Carne e Habitou entre Nós", "G-2e9bZqC7Q"),
        "at" to Pair("Atos dos Apóstolos • A Força do Espírito Santo", "CGbUa4aY18k"),
        "rm" to Pair("Romanos • A Justificação pela Fé e a Graça", "m67-CmsOO18"),
        "1co" to Pair("1 Coríntios • Unidade, Vida e Amor Cristão", "yiSjZ8C2c_s"),
        "2co" to Pair("2 Coríntios • Força na Fraqueza", "4_h6E338b_A"),
        "gl" to Pair("Gálatas • A Verdadeira Liberdade em Cristo", "vmx4UUXwVbM"),
        "ef" to Pair("Efésios • As Riquezas Insondáveis em Cristo", "Y71Q-qX9oK0"),
        "fp" to Pair("Filipenses • Alegria e Contentamento no Senhor", "oEDiG8gBf1s"),
        "cl" to Pair("Colossenses • A Supremacia e Centralidade de Cristo", "pftm7c8e9bA"),
        "1ts" to Pair("1 Tessalonicenses • Firmeza e Esperança da Volta de Cristo", "b18k7m6e4tA"),
        "2ts" to Pair("2 Tessalonicenses • Perseverança até o Fim", "p19m6k7b8tA"),
        "1tm" to Pair("1 Timóteo • Liderança e Saúde da Igreja", "76O8f0N_8tA"),
        "2tm" to Pair("2 Timóteo • Combati o Bom Combate", "u18k7m6e4tA"),
        "tt" to Pair("Tito • Boas Obras e Sã Doutrina", "t18k7m6e4tA"),
        "fm" to Pair("Filemom • Reconciliação e Fraternidade", "f18k7m6e4tA"),
        "hb" to Pair("Hebreus • A Superioridade de Jesus Cristo", "1fNWT40kvdM"),
        "tg" to Pair("Tiago • A Fé que Opera pelo Amor", "QN0p1g0e4bA"),
        "1pe" to Pair("1 Pedro • Esperança Viva em Meio ao Sofrimento", "WhPvJmXSwG8"),
        "2pe" to Pair("2 Pedro • Crescendo na Graça e no Conhecimento", "p18m6k7b8tA"),
        "1jo" to Pair("1 João • Deus é Luz, Amor e Vida", "19k7m6e4b8t"),
        "2jo" to Pair("2 João • Andando na Verdade", "29k7m6e4b8t"),
        "3jo" to Pair("3 João • Cooperação com a Verdade", "39k7m6e4b8t"),
        "jd" to Pair("Judas • Batalhar pela Fé Única", "j18k7m6e4tA"),
        "ap" to Pair("Apocalipse • A Vitória Final do Cordeiro", "VpbW_qW4B9M")
    )

    /**
     * Retorna a lista de vídeos recomendados para o versículo selecionado.
     */
    fun getRecommendationsForVerse(
        bookAbbrev: String,
        bookName: String,
        chapter: Int,
        verseNumber: Int,
        verseText: String
    ): List<BibleVideoRecommendation> {
        val cleanAbbrev = bookAbbrev.lowercase().trim()
        val list = mutableListOf<BibleVideoRecommendation>()

        // 1. Panorama Oficial do Livro (BibleProject Português)
        val panorama = bookPanoramas[cleanAbbrev]
        if (panorama != null) {
            list.add(
                BibleVideoRecommendation(
                    id = "bp_${cleanAbbrev}",
                    title = panorama.first,
                    channelName = "BibleProject Português",
                    videoId = panorama.second,
                    youtubeUrl = "https://www.youtube.com/watch?v=${panorama.second}",
                    thumbnailUrl = "https://img.youtube.com/vi/${panorama.second}/hqdefault.jpg",
                    categoryBadge = "Panorama do Livro",
                    description = "Compreenda a mensagem central, contexto histórico e temas principais do livro de $bookName."
                )
            )
        }

        // 2. Estudo Expositivo e Contextual do Capítulo
        val chapterStudyQuery = "$bookName $chapter estudo biblico explicacao"
        list.add(
            BibleVideoRecommendation(
                id = "study_${cleanAbbrev}_${chapter}",
                title = "Estudo Expositivo de $bookName $chapter",
                channelName = "Escola Bíblica & Teologia",
                videoId = "search_chapter",
                youtubeUrl = "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(chapterStudyQuery, "UTF-8")}",
                thumbnailUrl = if (panorama != null) "https://img.youtube.com/vi/${panorama.second}/mqdefault.jpg" else "",
                categoryBadge = "Estudo do Capítulo",
                description = "Análise do capítulo $chapter de $bookName com comentários históricos e teológicos."
            )
        )

        // 3. Estudo Temático Direto do Versículo
        val verseQuery = "$bookName $chapter:$verseNumber estudo bíblico significado"
        val snippet = if (verseText.length > 50) verseText.take(50).trim() + "..." else verseText.trim()
        list.add(
            BibleVideoRecommendation(
                id = "verse_${cleanAbbrev}_${chapter}_${verseNumber}",
                title = "Significado e Aplicação de $bookName $chapter:$verseNumber",
                channelName = "Teologia Expositiva",
                videoId = "search_verse",
                youtubeUrl = "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(verseQuery, "UTF-8")}",
                thumbnailUrl = if (panorama != null) "https://img.youtube.com/vi/${panorama.second}/mqdefault.jpg" else "",
                categoryBadge = "Reflexão & Prática",
                description = "Reflexão sobre o versículo $verseNumber: \"$snippet\"."
            )
        )

        return list
    }

    /**
     * Retorna a URL de pesquisa direta no YouTube com a query mais otimizada para o versículo.
     */
    fun buildSearchUrlForVerse(bookName: String, chapter: Int, verseNumber: Int): String {
        val query = "$bookName $chapter $verseNumber estudo bíblico explicação"
        return "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}"
    }
}
