package com.example.recipelibrary

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.data.FileTextExtractor
import com.example.recipelibrary.data.FileTextExtractor.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class FileTextExtractorTest {

    private fun docx(documentXml: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use {
            it.putNextEntry(ZipEntry("word/document.xml"))
            it.write(documentXml.toByteArray())
            it.closeEntry()
        }
        return bytes.toByteArray()
    }

    @Test
    fun docx_extractsParagraphsOnSeparateLines() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>
            <w:p><w:r><w:t>Tarte aux pommes</w:t></w:r></w:p>
            <w:p><w:r><w:t>200 g de </w:t></w:r><w:r><w:t>farine</w:t></w:r></w:p>
            </w:body></w:document>"""
        val text = FileTextExtractor.extractDocx(ByteArrayInputStream(docx(xml)))
        assertEquals("Tarte aux pommes\n200 g de farine", text)
    }

    @Test(expected = FileTextExtractor.UnsupportedFormatException::class)
    fun docx_withoutDocumentXml_isRejected() {
        FileTextExtractor.extractDocx(ByteArrayInputStream(ByteArray(0)))
    }

    @Test
    fun detectFormat_usesMimeThenExtension() {
        assertEquals(Format.PDF, FileTextExtractor.detectFormat("application/pdf", null))
        assertEquals(Format.DOCX, FileTextExtractor.detectFormat("application/octet-stream", "recette.DOCX"))
        assertEquals(Format.TXT, FileTextExtractor.detectFormat(null, "recette.txt"))
        assertNull(FileTextExtractor.detectFormat("image/png", "photo.png"))
    }
}
