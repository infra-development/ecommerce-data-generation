package com.shopsphere.datagenerator.reference

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{ObjectMapper, PropertyNamingStrategies}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.nio.file.{Files, Paths}

object GeographyLoader {

  private val mapper =
    new ObjectMapper()
      .setPropertyNamingStrategy(
        PropertyNamingStrategies.SNAKE_CASE
      ).registerModule(DefaultScalaModule)

  def load(baseDirectory: String): GeographyReferenceData = {

    val countries =
      loadFile[Country](
        baseDirectory,
        "countries.json",
        new TypeReference[Seq[Country]] {}
      )

    val states =
      loadFile[State](
        baseDirectory,
        "states.json",
        new TypeReference[Seq[State]] {}
      )

    val cities =
      loadFile[City](
        baseDirectory,
        "cities.json",
        new TypeReference[Seq[City]] {}
      )

    val areas =
      loadFile[Area](
        baseDirectory,
        "areas.json",
        new TypeReference[Seq[Area]] {}
      )

    val roads =
      loadFile[Road](
        baseDirectory,
        "roads.json",
        new TypeReference[Seq[Road]] {}
      )

    val societies =
      loadFile[Society](
        baseDirectory,
        "societies.json",
        new TypeReference[Seq[Society]] {}
      )

    val buildings =
      loadFile[Building](
        baseDirectory,
        "buildings.json",
        new TypeReference[Seq[Building]] {}
      )

    val postalCodes =
      loadFile(
        baseDirectory,
        "postal_codes.json",
        new TypeReference[Seq[PostalCode]] {}
      )

    GeographyValidator.validate(
      countries,
      states,
      cities,
      areas,
      roads,
      societies,
      buildings,
      postalCodes
    )

    GeographyReferenceData(
      countries = countries.map(value => value.id -> value).toMap,
      states = states.map(value => value.id -> value).toMap,
      cities = cities.map(value => value.id -> value).toMap,
      areas = areas.map(value => value.id -> value).toMap,
      roads = roads.map(value => value.id -> value).toMap,
      societies = societies.map(value => value.id -> value).toMap,
      buildings = buildings.map(value => value.id -> value).toMap,
      postalCodes = postalCodes.map(value => value.id -> value).toMap
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
        s"Reference data file does not exist: $path"
      )
    }

    mapper.readValue(
      Files.readString(path),
      typeReference
    )
  }

}