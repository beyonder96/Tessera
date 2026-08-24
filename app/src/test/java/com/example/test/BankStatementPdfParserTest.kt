package com.example.test

import com.example.utils.BankStatementPdfParser
import org.junit.Assert.*
import org.junit.Test

class BankStatementPdfParserTest {

    @Test
    fun testParseNubankStatementSample() {
        val sampleLines = listOf(
            "EXTRATO DE CONTA DO NUBANK",
            "Período: 01/08/2026 a 22/08/2026",
            "02/08/2026 - Transferência recebida pelo Pix - João Silva R$ 1.500,00",
            "05/08/2026 - Compra no débito - Supermercado Pão de Açúcar R$ 245,80",
            "10/08/2026 - Pagamento de fatura - Cartão Nubank R$ 820,50",
            "15/08/2026 - Transferência enviada pelo Pix - Posto Ipiranga R$ 150,00",
            "20/08/2026 - Rendimento de conta R$ 18,45",
            "Saldo final disponível: R$ 3.420,15"
        )

        val transactions = BankStatementPdfParser.extractTransactionsFromLines(sampleLines)

        assertEquals(5, transactions.size)

        // 1. Pix Recebido (Receita)
        val t1 = transactions[0]
        assertTrue(t1.isIncome)
        assertEquals(1500.00, t1.amount, 0.01)
        assertTrue(t1.title.contains("Transferência", ignoreCase = true) || t1.title.contains("João Silva", ignoreCase = true))

        // 2. Supermercado (Despesa, Alimentação)
        val t2 = transactions[1]
        assertFalse(t2.isIncome)
        assertEquals(245.80, t2.amount, 0.01)
        assertEquals("Alimentação", t2.category)

        // 3. Posto Ipiranga (Despesa, Transporte)
        val t4 = transactions[3]
        assertFalse(t4.isIncome)
        assertEquals(150.00, t4.amount, 0.01)
        assertEquals("Transporte", t4.category)

        // 4. Rendimento (Receita, Investimentos)
        val t5 = transactions[4]
        assertTrue(t5.isIncome)
        assertEquals(18.45, t5.amount, 0.01)
        assertEquals("Investimentos", t5.category)
    }

    @Test
    fun testParseItauStatementSampleWithIndicators() {
        val sampleLines = listOf(
            "BANCO ITAU S.A. - EXTRATO MENSAL",
            "03/08 PIX TRANSF FULANO 300,00 D",
            "04/08 SALARIO EMPRESA XYZ 5.200,00 C",
            "08/08 COMPRA ELETRO FARMACIA DROGASIL 89,90 -",
            "12/08 ENEL CONTA DE LUZ 142,30 D"
        )

        val transactions = BankStatementPdfParser.extractTransactionsFromLines(sampleLines)

        assertEquals(4, transactions.size)

        val pixTx = transactions[0]
        assertFalse(pixTx.isIncome)
        assertEquals(300.00, pixTx.amount, 0.01)

        val salaryTx = transactions[1]
        assertTrue(salaryTx.isIncome)
        assertEquals(5200.00, salaryTx.amount, 0.01)
        assertEquals("Salário", salaryTx.category)

        val pharmaTx = transactions[2]
        assertFalse(pharmaTx.isIncome)
        assertEquals(89.90, pharmaTx.amount, 0.01)
        assertEquals("Saúde", pharmaTx.category)

        val enelTx = transactions[3]
        assertFalse(enelTx.isIncome)
        assertEquals(142.30, enelTx.amount, 0.01)
        assertEquals("Moradia", enelTx.category)
    }

    @Test
    fun testParseRealItauStatementFromPdf() {
        val rawPdfLines = listOf(
            "KENNED FERREIRA OLIVEIRA 436.374.578-90 agência: 0057 conta: 018231-3",
            "saldo em conta Limite da Conta utilizado Limite da Conta disponível Limite da Conta total *",
            "R$ 8,83 R$ 0,00 R$ 0,00 R$ 0,00",
            "* Total contratado. O uso do Limite da Conta e Limite da Conta adicional poderá ter cobrança de juros + IOF.",
            "extrato conta / lançamentos",
            "período de visualização: 24/07/2026 até 23/08/2026 emitido em: 23/08/2026 09:20:37",
            "data lançamentos valor (R$) saldo (R$)",
            "24/08/2026 PIX TRANSF NICOLI 22/08 30,00",
            "22/08/2026 SALDO DO DIA 8,83",
            "21/08/2026 PIX TRANSF NICOLI 21/08 30,00",
            "21/08/2026 PIX AUT GOOGLE BRASIL PA 21/08 -16,90",
            "21/08/2026 RSHOP BOTECO CARNA2108 -9,00",
            "21/08/2026 RSHOP EmporioPdoce2108 -3,75",
            "21/08/2026 ON 99Food RAW 21/08 -10,98",
            "21/08/2026 RSHOP CARREFOUR SP2108 -11,38 -21,17",
            "21/08/2026 SALDO DO DIA -21,17",
            "20/08/2026 PIX TRANSF NICOLI 20/08 -77,00",
            "20/08/2026 SALDO DO DIA 0,84",
            "20/08/2026 RSHOP BOTECO CARNA2008 -9,00",
            "20/08/2026 REMUNERACAO/SALARIO 1.600,00",
            "20/08/2026 PIX TRANSF NICOLI 20/08 -1.500,00",
            "19/08/2026 RSHOP NAKATA CAFE 1908 -24,00",
            "19/08/2026 SALDO DO DIA -13,16",
            "18/08/2026 RSHOP MIGUEL E FIL1808 -15,00",
            "18/08/2026 RSHOP ESSENCIAL SA1808 -2,90",
            "18/08/2026 RSHOP CARREFOUR SP1808 -10,78",
            "18/08/2026 RSHOP CARREFOUR SP1808 -11,90",
            "18/08/2026 RSHOP JK MINI MERC1808 -3,00",
            "18/08/2026 SALDO DO DIA 10,84",
            "17/08/2026 PIX TRANSF KENNED 15/08 5,03",
            "17/08/2026 PIX TRANSF NICOLI 16/08 100,00",
            "17/08/2026 RSHOP GILSON DA SI1608 -10,00",
            "17/08/2026 RSHOP EDILSON BERN1608 -10,00",
            "17/08/2026 RSHOP MP 4HERDEIR1608 -10,00",
            "17/08/2026 RSHOP THAMIRES DOS1608 -15,00",
            "17/08/2026 RSHOP SeverinoSeba1608 -10,00",
            "17/08/2026 ON 99Food A a 17/08 -2,28",
            "17/08/2026 RSHOP PAES E DOCES1708 -8,00",
            "17/08/2026 SALDO DO DIA 54,42",
            "14/08/2026 RSHOP MP JOANADAR1408 -4,00",
            "14/08/2026 RSHOP CARREFOUR SP1408 -45,46",
            "14/08/2026 ON Uber UBER 14/08 -10,95",
            "14/08/2026 EST ON Uber UBER 10,95",
            "14/08/2026 SALDO DO DIA 14,67",
            "13/08/2026 RSHOP METRO FARMA 1308 -9,99",
            "13/08/2026 RSHOP DOCE SONHO D1308 -1,99",
            "13/08/2026 RSHOP EmporioPdoce1308 -6,00",
            "13/08/2026 SALDO DO DIA 64,13",
            "12/08/2026 PIX TRANSF 58.965.12/08 -1.500,00",
            "12/08/2026 PIX TRANSF DENIS F12/08 -11,00",
            "12/08/2026 SISPAG TELL COM PRESENTE 50,59",
            "12/08/2026 SALDO DO DIA 82,11",
            "11/08/2026 DEV PIX PIX Marketp11/08 1.500,00",
            "11/08/2026 SALDO DO DIA 1.542,52",
            "10/08/2026 PIX QRS 99 FOOD08/08 -31,49",
            "10/08/2026 RSCSS DBMS COMERCI0808 -5,50",
            "10/08/2026 RSCSS ElitonAparec0808 -16,00",
            "10/08/2026 PIX QRS SHPP BRASIL08/08 -36,99",
            "10/08/2026 PIX QRS VIVARA08/08 -25,00",
            "10/08/2026 ON Uber UBER 09/08 -0,82",
            "10/08/2026 RSCSS SESC BELENZI0908 -11,00",
            "10/08/2026 RSCSS SONEDA PERFU0908 -15,99",
            "10/08/2026 PIX AUT SABESP 10/08 -17,56",
            "10/08/2026 PIX QRS 99 TECNOLOG10/08 -8,69",
            "10/08/2026 ON Uber UBER 10/08 -9,73",
            "10/08/2026 SALDO DO DIA 42,52",
            "07/08/2026 PIX QRS GOOGLE BRAS07/08 -16,90",
            "07/08/2026 PIX TRANSF ZILENE 07/08 -127,00",
            "07/08/2026 PAY Uber 07/08 -12,34",
            "07/08/2026 PIX QRS 99 FOOD07/08 -35,16",
            "07/08/2026 SALDO DO DIA 221,29",
            "06/08/2026 PIX TRANSF DENIS F06/08 140,00",
            "06/08/2026 RSCSS PIZZA HUT IB0608 -22,90",
            "06/08/2026 RSCSS TARIFA TRANS0608 -150,00",
            "06/08/2026 PIX TRANSF Kenned 06/08 408,05",
            "06/08/2026 PIX TRANSF Kenned 06/08 2,38",
            "06/08/2026 SALDO DO DIA 412,69",
            "05/08/2026 PIX TRANSF NICOLI 05/08 8,00",
            "05/08/2026 REMUNERACAO/SALARIO 1.012,37",
            "05/08/2026 RSCSS PAES E DOCES0508 -4,50",
            "05/08/2026 PIX TRANSF NICOLI 05/08 150,00",
            "05/08/2026 PIX QRS PLANO CARVA05/08 -1.122,96",
            "05/08/2026 SALDO DO DIA 35,16",
            "04/08/2026 ON DM Spotify 04/08 -12,90",
            "04/08/2026 SALDO DO DIA -7,75",
            "03/08/2026 PIX QRS PIX Marketp02/08 -1.500,00",
            "03/08/2026 REND PAGO APLIC AUT MAIS 0,01",
            "03/08/2026 SALDO DO DIA 5,15",
            "31/07/2026 RSCSS MP JOANADAR3107 -4,00",
            "31/07/2026 PIX QRS UBER DO BRA31/07 -8,20",
            "31/07/2026 SALDO DO DIA 1.505,14",
            "30/07/2026 RSCSS MP JOANADAR3007 -4,00",
            "30/07/2026 PIX QRS UBER DO BRA30/07 -8,95",
            "30/07/2026 PIX QRS UBER DO BRA30/07 -8,21",
            "30/07/2026 SALDO DO DIA 1.517,34",
            "29/07/2026 PIX QRS UBER DO BRA29/07 -8,47",
            "29/07/2026 DEV PIX GRUPO CASAS29/07 1.500,00",
            "29/07/2026 RSCSS LOJAS AMERIC2907 -15,98",
            "29/07/2026 RSCSS TARIFA TRANS2907 -50,00",
            "29/07/2026 ESTORNO RSCSS TARIFA TRANS 50,00",
            "29/07/2026 RSCSS TARIFA TRANS2907 -50,00",
            "29/07/2026 SALDO DO DIA 1.538,50",
            "28/07/2026 PIX QRS 99 TECNOLOG28/07 -10,60",
            "28/07/2026 RSCSS EmporioPdoce2807 -9,50",
            "28/07/2026 SALDO DO DIA 112,95",
            "27/07/2026 PIX QRS MARIA PHILO27/07 -4,50",
            "27/07/2026 RSCSS PADARIA JUNI2707 -10,00",
            "27/07/2026 SALDO DO DIA 133,05",
            "27/07/2026 PIX TRANSF Nicoli 25/07 -100,00",
            "27/07/2026 ON IFD IFOOD C 25/07 -7,95",
            "27/07/2026 PIX QRS IFOOD.COM A25/07 -33,82",
            "27/07/2026 PIX QRS IFOOD.COM A25/07 -26,48",
            "27/07/2026 PIX QRS IFOOD.COM A25/07 -6,98",
            "27/07/2026 ON Uber UBER 26/07 -13,23",
            "27/07/2026 EST ON Uber UBER 13,23",
            "27/07/2026 ON Uber UBER 26/07 -13,34",
            "27/07/2026 RSCSS MP ALEMAOBA2607 -46,00",
            "24/07/2026 PIX TRANSF KENNED 24/07 -15,00",
            "24/07/2026 PIX TRANSF NICOLI 24/07 -20,00",
            "24/07/2026 PIX QRS ITAU UNIBAN24/07 -3.607,77",
            "24/07/2026 PIX TRANSF NICOLI 24/07 -1.600,00",
            "24/07/2026 PIX TRANSF KENNED 24/07 -6,00",
            "24/07/2026 PIX QRS IFOOD.COM A24/07 -30,88",
            "24/07/2026 PIX QRS ENEL DISTRI24/07 -75,14",
            "24/07/2026 TED 121.0000BANCO AGIBAN 5.519,56",
            "24/07/2026 SALDO DO DIA 382,12",
            "23/07/2026 SALDO DO DIA 217,35",
            "Aviso!",
            "Os saldos acima são baseados nas informações disponíveis até esse instante..."
        )

        val transactions = BankStatementPdfParser.extractTransactionsFromLines(rawPdfLines)

        // 1. Verifica que nenhuma linha de SALDO DO DIA ou metadados foi inserida
        assertTrue(transactions.none { it.title.contains("saldo", ignoreCase = true) })
        assertTrue(transactions.none { it.title.contains("limite", ignoreCase = true) })
        assertTrue(transactions.none { it.title.contains("aviso", ignoreCase = true) })

        // 2. Verifica linha com duas colunas (valor -11,38 e saldo -21,17): deve pegar 11.38
        val carrefourLine = transactions.find { it.title.contains("Carrefour", ignoreCase = true) && it.dateFormatted == "21/08" }
        assertNotNull(carrefourLine)
        assertEquals(11.38, carrefourLine!!.amount, 0.01)
        assertFalse(carrefourLine.isIncome)

        // 3. Validação de Entradas / Receitas do extrato
        val pixNicoli24 = transactions.find { it.title.contains("Nicoli", ignoreCase = true) && it.dateFormatted == "24/08" }
        assertNotNull(pixNicoli24)
        assertTrue(pixNicoli24!!.isIncome)
        assertEquals(30.00, pixNicoli24.amount, 0.01)

        val salario = transactions.find { it.title.contains("Remuneracao", ignoreCase = true) && it.amount == 1600.00 }
        assertNotNull(salario)
        assertTrue(salario!!.isIncome)
        assertEquals("Salário", salario.category)

        val devPix = transactions.find { it.title.contains("Dev Pix", ignoreCase = true) && it.amount == 1500.00 }
        assertNotNull(devPix)
        assertTrue(devPix!!.isIncome)
        assertEquals("Reembolso", devPix.category)

        val estorno = transactions.find { it.title.contains("Estorno", ignoreCase = true) && it.amount == 50.00 }
        assertNotNull(estorno)
        assertTrue(estorno!!.isIncome)
        assertEquals("Reembolso", estorno.category)

        val ted = transactions.find { it.title.contains("Agiban", ignoreCase = true) }
        assertNotNull(ted)
        assertTrue(ted!!.isIncome)
        assertEquals(5519.56, ted.amount, 0.01)

        val rendimento = transactions.find { it.title.contains("Rend Pago", ignoreCase = true) }
        assertNotNull(rendimento)
        assertTrue(rendimento!!.isIncome)
        assertEquals(0.01, rendimento.amount, 0.001)
        assertEquals("Investimentos", rendimento.category)

        // 4. Validação de Saídas / Despesas do extrato
        val pixSaida = transactions.find { it.title.contains("Nicoli", ignoreCase = true) && it.amount == 1500.00 }
        assertNotNull(pixSaida)
        assertFalse(pixSaida!!.isIncome)

        val enel = transactions.find { it.title.contains("Enel", ignoreCase = true) }
        assertNotNull(enel)
        assertFalse(enel!!.isIncome)
        assertEquals(75.14, enel.amount, 0.01)
        assertEquals("Moradia", enel.category)

        val spotify = transactions.find { it.title.contains("Spotify", ignoreCase = true) }
        assertNotNull(spotify)
        assertFalse(spotify!!.isIncome)
        assertEquals(12.90, spotify.amount, 0.01)
        assertEquals("Lazer", spotify.category)
    }

    @Test
    fun testParseTesseraExportedPdfWithCategories() {
        val tesseraExportLines = listOf(
            "Extrato Tessera - Agosto 2026",
            "Data Título Categoria Conta/Cartão Valor",
            "----------------------------------------------------------",
            "01/08/2026 Salário Mensal Salário Nubank +R$ 5.000,00",
            "03/08/2026 Aluguel Apartamento Moradia Itaú -R$ 1.800,00",
            "05/08/2026 Supermercado Pão Açúcar Alimentação C6 Carbon -R$ 450,20",
            "10/08/2026 Consulta Médica Saúde Nubank -R$ 250,00",
            "15/08/2026 Ração Golden Petz Inter -R$ 189,90"
        )

        val transactions = BankStatementPdfParser.extractTransactionsFromLines(tesseraExportLines)

        assertEquals(5, transactions.size)

        // 1. Salário
        val t1 = transactions[0]
        assertEquals("Salário Mensal", t1.title)
        assertEquals("Salário", t1.category)
        assertTrue(t1.isIncome)
        assertEquals(5000.00, t1.amount, 0.01)

        // 2. Aluguel
        val t2 = transactions[1]
        assertEquals("Aluguel Apartamento", t2.title)
        assertEquals("Moradia", t2.category)
        assertFalse(t2.isIncome)
        assertEquals(1800.00, t2.amount, 0.01)

        // 3. Supermercado
        val t3 = transactions[2]
        assertEquals("Supermercado Pão Açúcar", t3.title)
        assertEquals("Alimentação", t3.category)
        assertFalse(t3.isIncome)
        assertEquals(450.20, t3.amount, 0.01)

        // 4. Consulta Médica
        val t4 = transactions[3]
        assertEquals("Consulta Médica", t4.title)
        assertEquals("Saúde", t4.category)
        assertFalse(t4.isIncome)
        assertEquals(250.00, t4.amount, 0.01)

        // 5. Petz
        val t5 = transactions[4]
        assertEquals("Ração Golden", t5.title)
        assertEquals("Petz", t5.category)
        assertFalse(t5.isIncome)
        assertEquals(189.90, t5.amount, 0.01)
    }

    @Test
    fun testPixExpenseDoesNotBecomeTransfer() {
        val lines = listOf(
            "02/08/2026 PIX PAGTO MERCADINHO VILA -45,00",
            "03/08/2026 PIX ENVIADO JOAO SILVA -120,00"
        )

        val transactions = BankStatementPdfParser.extractTransactionsFromLines(lines)
        assertEquals(2, transactions.size)

        // PIX Mercadinho deve ser Alimentação
        val t1 = transactions[0]
        assertFalse(t1.isIncome)
        assertEquals(45.00, t1.amount, 0.01)
        assertEquals("Alimentação", t1.category)

        // PIX genérico para pessoa sem loja conhecida deve ser "Outros" (e não "Transferência"),
        // para não sumir do cálculo de gastos comprometidos
        val t2 = transactions[1]
        assertFalse(t2.isIncome)
        assertEquals(120.00, t2.amount, 0.01)
        assertNotEquals("Transferência", t2.category)
    }
}
