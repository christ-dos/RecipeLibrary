package com.example.recipelibrary.nutrition

import com.example.recipelibrary.data.nutrition.Normalize
import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeTest {
    @Test fun lowercaseAndAccents() = assertEquals("creme fraiche epaisse", Normalize.text("Crème Fraîche ÉPAISSE"))
    @Test fun ligatures() = assertEquals("oeuf", Normalize.text("Œuf"))
    @Test fun ligatureAe() = assertEquals("caecum", Normalize.text("cæcum"))
    @Test fun apostrophesUnified() = assertEquals(Normalize.text("huile d'olive"), Normalize.text("huile d’olive"))
    @Test fun pluralS() = assertEquals("pomme de terre", Normalize.text("pommes de terre"))
    @Test fun pluralX() = assertEquals("chou", Normalize.text("choux"))
    @Test fun pluralAux() = assertEquals("cheval", Normalize.text("chevaux"))
    @Test fun shortWordsKept() = assertEquals("de la", Normalize.text("de la"))
    @Test fun punctuationSplitsWords() = assertEquals("beurre demi sel", Normalize.text("beurre demi-sel,"))
    @Test fun plainKeepsPunctuation() = assertEquals("1,5 kg d'oeufs", Normalize.plain("1,5 kg d’Œufs"))
}
