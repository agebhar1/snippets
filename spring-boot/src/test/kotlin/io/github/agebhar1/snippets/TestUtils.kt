package io.github.agebhar1.snippets

import org.xml.sax.InputSource

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

internal fun String.toDocument() =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(this)))
