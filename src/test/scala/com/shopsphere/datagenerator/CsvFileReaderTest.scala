package com.shopsphere.datagenerator.validation

import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class CsvFileReaderTest
  extends AnyFunSuite {

  test("CSV reader should read normal CSV data") {

    val directory =
      Files.createTempDirectory(
        "shopsphere-reader-test"
      )

    val file =
      directory.resolve("test.csv")

    Files.writeString(
      file,
      "id,name\n1,Alice\n2,Bob\n",
      StandardCharsets.UTF_8
    )

    val table =
      CsvFileReader.read(file)

    assert(
      table.header ==
        Seq("id", "name")
    )

    assert(
      table.rows.size == 2
    )

    assert(
      table.rows.head("name") == "Alice"
    )
  }

  test("CSV reader should handle quoted commas and quotes") {

    val directory =
      Files.createTempDirectory(
        "shopsphere-reader-test"
      )

    val file =
      directory.resolve("test.csv")

    Files.writeString(
      file,
      "id,name\n" +
        "1,\"Smith, John\"\n" +
        "2,\"John \"\"The Boss\"\" Smith\"\n",
      StandardCharsets.UTF_8
    )

    val table =
      CsvFileReader.read(file)

    assert(
      table.rows(0)("name") ==
        "Smith, John"
    )

    assert(
      table.rows(1)("name") ==
        """John "The Boss" Smith"""
    )
  }

  test("CSV reader should reject inconsistent column count") {

    val directory =
      Files.createTempDirectory(
        "shopsphere-reader-test"
      )

    val file =
      directory.resolve("test.csv")

    Files.writeString(
      file,
      "id,name\n1,Alice,Extra\n",
      StandardCharsets.UTF_8
    )

    assertThrows[IllegalArgumentException] {
      CsvFileReader.read(file)
    }
  }
}