package com.shopsphere.datagenerator.reference.customer

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.nio.file.{Files, Paths}

object CustomerReferenceLoader {

  private val mapper =
    new ObjectMapper()
      .registerModule(DefaultScalaModule)

  def load(
            baseDirectory: String
          ): CustomerReferenceData = {

    val firstNames =
      loadFile[String](
        baseDirectory,
        "first_names.json",
        new TypeReference[Seq[String]] {}
      )

    val lastNames =
      loadFile[String](
        baseDirectory,
        "last_names.json",
        new TypeReference[Seq[String]] {}
      )

    if (firstNames.isEmpty) {
      throw new IllegalArgumentException(
        "Customer first-name reference data must not be empty."
      )
    }

    if (lastNames.isEmpty) {
      throw new IllegalArgumentException(
        "Customer last-name reference data must not be empty."
      )
    }

    CustomerReferenceData(
      firstNames = firstNames,
      lastNames = lastNames
    )
  }

  private def loadFile[T](
                           baseDirectory: String,
                           fileName: String,
                           typeReference: TypeReference[Seq[T]]
                         ): Seq[T] = {

    val path =
      Paths.get(baseDirectory, fileName)

    if (!Files.exists(path)) {
      throw new IllegalArgumentException(
        s"Customer reference data file does not exist: $path"
      )
    }

    mapper.readValue(
      Files.readString(path),
      typeReference
    )
  }
}