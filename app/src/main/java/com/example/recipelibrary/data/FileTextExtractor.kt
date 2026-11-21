package com.example.recipelibrary.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Types de fichiers acceptés par l'import de recette. */
object FileTextExtractor {

    const val MIME_TXT = "text/plain"
    const val MIME_PDF = "application/pdf"
    const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    val SUPPORTED_MIME_TYPES = arrayOf(MIME_TXT, MIME_PDF, MIME_DOCX)

    enum class Format { TXT, PDF, DOCX }

    class UnsupportedFormatException : Exception("Format de fichier non pris en charge")

    /** Extrait le texte du fichier désigné par [uri]. Appel bloquant : à lancer hors thread UI. */
    fun extract(context: Context, uri: Uri): String {
        val format = detectFormat(context.contentResolver.getType(uri), displayName(context, uri))
            ?: throw UnsupportedFormatException()
        val input = context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("Fichier illisible")
        return input.use {
            when (format) {
                Format.TXT -> it.bufferedReader().readText()
                Format.DOCX -> extractDocx(it)
                Format.PDF -> {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    extractPdf(it)
                }
            }
        }
    }

    fun detectFormat(mime: String?, fileName: String?): Format? {
        when (mime) {
            MIME_TXT -> return Format.TXT
            MIME_PDF -> return Format.PDF
            MIME_DOCX -> return Format.DOCX
        }
        // Certains fournisseurs renvoient application/octet-stream : on se rabat sur l'extension.
        return when (fileName?.substringAfterLast('.', "")?.lowercase()) {
            "txt" -> Format.TXT
            "pdf" -> Format.PDF
            "docx" -> Format.DOCX
            else -> null
        }
    }

    fun extractPdf(input: InputStream): String =
        PDDocument.load(input).use { PDFTextStripper().getText(it) }

    /** Lit `word/document.xml` dans le zip du .docx et reconstitue le texte paragraphe par paragraphe. */
    fun extractDocx(input: InputStream): String {
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "word/document.xml") return parseDocxXml(zip)
            }
        }
        throw UnsupportedFormatException()
    }

    private fun parseDocxXml(stream: InputStream): String {
        val parser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")
        val out = StringBuilder()
        var inText = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "t" -> inText = true
                    "tab" -> out.append('\t')
                    "br" -> out.append('\n')
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inText = false
                    "p" -> out.append('\n')
                }
                XmlPullParser.TEXT -> if (inText) out.append(parser.text)
            }
            event = parser.next()
        }
        return out.toString().trim()
    }

    private fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
}
