package com.shopsphere.datagenerator.output

import org.scalatest.funsuite.AnyFunSuite

import java.nio.file.Files

class CsvWriterTest extends AnyFunSuite {

  private val lineSeparator =
    System.lineSeparator()

  test("CSV writer should write header and rows") {

    val directory =
      Files.createTempDirectory(
        "shopsphere-csv-test"
      )

    val file =
      directory.resolve("test.csv")

    val count =
      CsvWriter.write(
        filePath = file,
        header = Seq(
          "id",
          "name"
        ),
        rows = Iterator(
          Seq("1", "Alice"),
          Seq("2", "Bob")
        )
      )

    assert(count == 2)

    val content =
      Files.readString(file)

    assert(
      content ==
        "id,name" + lineSeparator +
          "1,Alice" + lineSeparator +
          "2,Bob" + lineSeparator
    )
  }

  test("CSV writer should escape commas and quotes") {

    val directory =
      Files.createTempDirectory(
        "shopsphere-csv-test"
      )

    val file =
      directory.resolve("test.csv")

    CsvWriter.write(
      filePath = file,
      header = Seq(
        "id",
        "name"
      ),
      rows = Iterator(
        Seq(
          "1",
          "Smith, John"
        ),
        Seq(
          "2",
          """John "The Boss" Smith"""
        )
      )
    )

    val content =
      Files.readString(file)

    assert(
      content ==
        "id,name" + lineSeparator +
          "1,\"Smith, John\"" + lineSeparator +
          "2,\"John \"\"The Boss\"\" Smith\"" +
          lineSeparator
    )
  }

  test("CSV writer should reject rows with wrong column count") {

    val directory =
      Files.createTempDirectory(
        "shopsphere-csv-test"
      )

    val file =
      directory.resolve("test.csv")

    assertThrows[IllegalArgumentException] {

      CsvWriter.write(
        filePath = file,
        header = Seq(
          "id",
          "name"
        ),
        rows = Iterator(
          Seq("1")
        )
      )
    }
  }
}