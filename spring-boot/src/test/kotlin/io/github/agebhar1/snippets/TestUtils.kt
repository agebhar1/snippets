package io.github.agebhar1.snippets

import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

internal fun String.toDocument() =
  DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(this)))
