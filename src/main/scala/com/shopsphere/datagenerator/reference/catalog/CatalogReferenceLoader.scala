package com.shopsphere.datagenerator.reference.catalog

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.nio.file.{Files, Paths}

object CatalogReferenceLoader {

  private val mapper =
    new ObjectMapper()
      .registerModule(DefaultScalaModule)

  def load(
            baseDirectory: String
          ): CatalogReferenceData = {

    val categories =
      loadFile[CategoryReference](
        baseDirectory,
        "categories.json",
        new TypeReference[Seq[CategoryReference]] {}
      )

    val brands =
      loadFile[BrandReference](
        baseDirectory,
        "brands.json",
        new TypeReference[Seq[BrandReference]] {}
      )

    if (categories.isEmpty) {
      throw new IllegalArgumentException(
        "Catalog category reference data must not be empty."
      )
    }

    if (brands.isEmpty) {
      throw new IllegalArgumentException(
        "Catalog brand reference data must not be empty."
      )
    }

    CatalogReferenceData(
      categories = categories,
      brands = brands
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
        s"Catalog reference data file does not exist: $path"
      )
    }

    mapper.readValue(
      Files.readString(path),
      typeReference
    )
  }
}