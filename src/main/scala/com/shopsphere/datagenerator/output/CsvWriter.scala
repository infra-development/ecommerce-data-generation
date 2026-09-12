package com.shopsphere.datagenerator.output

import java.nio.charset.StandardCharsets
import java.nio.file.{
  Files,
  Path,
  Paths,
  StandardOpenOption
}

object CsvWriter {

  def write(
             filePath: Path,
             header: Seq[String],
             rows: Iterator[Seq[String]]
           ): Long = {

    require(
      header.nonEmpty,
      "CSV header must not be empty."
    )

    val parent =
      filePath.getParent

    if (parent != null) {
      Files.createDirectories(parent)
    }

    val writer =
      Files.newBufferedWriter(
        filePath,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )

    try {

      writer.write(
        header.map(escape).mkString(",")
      )

      writer.newLine()

      var rowCount = 0L

      rows.foreach { row =>

        require(
          row.size == header.size,
          s"CSV row has ${row.size} columns but header has ${header.size} columns."
        )

        writer.write(
          row.map(escape).mkString(",")
        )

        writer.newLine()

        rowCount += 1
      }

      rowCount

    } finally {
      writer.close()
    }
  }

  private def escape(value: String): String = {

    if (value == null) {
      ""
    } else {

      val requiresQuoting =
        value.contains(",") ||
          value.contains("\"") ||
          value.contains("\n") ||
          value.contains("\r")

      if (requiresQuoting) {
        "\"" +
          value.replace("\"", "\"\"") +
          "\""
      } else {
        value
      }
    }
  }
}