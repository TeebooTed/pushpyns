package com.pushpyns.domain.pdf

sealed interface PdfBlock {
    data class Title(val text: String) : PdfBlock

    data class Subtitle(val text: String) : PdfBlock

    data class Section(val text: String) : PdfBlock

    data class KeyValue(val key: String, val value: String) : PdfBlock

    data class Paragraph(val text: String) : PdfBlock

    data class TableRow(val cells: List<String>, val header: Boolean = false) : PdfBlock

    data class Total(val label: String, val value: String) : PdfBlock

    data class Image(val relativePath: String, val caption: String) : PdfBlock

    data class Signature(val relativePath: String) : PdfBlock
}
