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
    fun testParseItauStatementSample() {
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
}
