package com.shopsphere.datagenerator.validation

import java.nio.charset.StandardCharsets
import java.nio.file.{
  Files,
  Path
}

case class CsvTable(
                     header: Seq[String],
                     rows: Seq[Map[String, String]]
                   )

object CsvFileReader {

  def read(
            path: Path
          ): CsvTable = {

    if (!Files.exists(path)) {
      throw new IllegalArgumentException(
        s"CSV file does not exist: $path"
      )
    }

    val lines =
      Files.readAllLines(
        path,
        StandardCharsets.UTF_8
      )

    if (lines.isEmpty) {
      throw new IllegalArgumentException(
        s"CSV file is empty: $path"
      )
    }

    val header =
      parseLine(lines.get(0))

    if (header.isEmpty) {
      throw new IllegalArgumentException(
        s"CSV header is empty: $path"
      )
    }

    val duplicateHeaders =
      header
        .groupBy(identity)
        .collect {
          case (name, values)
            if values.size > 1 =>
            name
        }

    if (duplicateHeaders.nonEmpty) {
      throw new IllegalArgumentException(
        s"Duplicate CSV headers in $path: " +
          duplicateHeaders.mkString(", ")
      )
    }

    val rows =
      (1 until lines.size())
        .map { index =>

          val values =
            parseLine(
              lines.get(index)
            )

          if (values.size != header.size) {
            throw new IllegalArgumentException(
              s"Invalid column count in $path at line ${index + 1}: " +
                s"expected ${header.size}, found ${values.size}"
            )
          }

          header
            .zip(values)
            .toMap
        }
        .toSeq

    CsvTable(
      header = header,
      rows = rows
    )
  }

  private def parseLine(
                         line: String
                       ): Seq[String] = {

    val values =
      scala.collection.mutable.ArrayBuffer.empty[String]

    val current =
      new StringBuilder

    var inQuotes = false
    var index = 0

    while (index < line.length) {

      val character =
        line.charAt(index)

      if (character == '"') {

        if (
          inQuotes &&
            index + 1 < line.length &&
            line.charAt(index + 1) == '"'
        ) {

          current.append('"')
          index += 1

        } else {

          inQuotes = !inQuotes
        }

      } else if (
        character == ',' &&
          !inQuotes
      ) {

        values += current.toString
        current.clear()

      } else {

        current.append(character)
      }

      index += 1
    }

    if (inQuotes) {
      throw new IllegalArgumentException(
        "Unterminated quoted CSV field."
      )
    }

    values += current.toString

    values.toSeq
  }
}