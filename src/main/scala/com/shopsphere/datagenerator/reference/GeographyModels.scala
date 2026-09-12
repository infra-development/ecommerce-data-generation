package com.shopsphere.datagenerator.reference

case class Country(
                    id: String,
                    name: String,
                    isoCode: String
                  )

case class State(
                  id: String,
                  countryId: String,
                  name: String,
                  code: String
                )

case class City(
                 id: String,
                 stateId: String,
                 name: String
               )

case class Area(
                 id: String,
                 cityId: String,
                 name: String
               )

case class Road(
                 id: String,
                 areaId: String,
                 name: String
               )

case class Society(
                    id: String,
                    roadId: String,
                    name: String
                  )

case class Building(
                     id: String,
                     societyId: String,
                     buildingNumber: String,
                     floors: Int,
                     unitsPerFloor: Int
                   )

case class PostalCode(
                       id: String,
                       areaId: String,
                       code: String
                     )