package com.shopsphere.datagenerator.config

sealed trait CardinalityProfile {
  def name: String
}

object CardinalityProfile {

  case object Low extends CardinalityProfile {
    override val name: String = "low"
  }

  case object Medium extends CardinalityProfile {
    override val name: String = "medium"
  }

  case object High extends CardinalityProfile {
    override val name: String = "high"
  }

  val all: Seq[CardinalityProfile] = Seq(
    Low,
    Medium,
    High
  )

  def fromString(value: String): Either[String, CardinalityProfile] = {
    all.find(_.name == value) match {
      case Some(profile) => Right(profile)

      case None =>
        Left(
          s"Unsupported cardinality profile: '$value'. " +
            s"Supported cardinality profiles: ${all.map(_.name).sorted.mkString(", ")}"
        )
    }
  }
}