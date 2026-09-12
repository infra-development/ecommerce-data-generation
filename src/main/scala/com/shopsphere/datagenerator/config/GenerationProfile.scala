package com.shopsphere.datagenerator.config

sealed trait GenerationProfile {
  def name: String
}

object GenerationProfile {

  case object Small extends GenerationProfile {
    override val name: String = "small"
  }

  case object Medium extends GenerationProfile {
    override val name: String = "medium"
  }

  case object Large extends GenerationProfile {
    override val name: String = "large"
  }

  case object XLarge extends GenerationProfile {
    override val name: String = "xlarge"
  }

  val all: Seq[GenerationProfile] = Seq(
    Small,
    Medium,
    Large,
    XLarge
  )

  def fromString(value: String): Either[String, GenerationProfile] = {
    all.find(_.name == value) match {
      case Some(profile) => Right(profile)
      case None =>
        Left(
          s"Unsupported generation profile: '$value'. " +
            s"Supported profiles: ${all.map(_.name).sorted.mkString(", ")}"
        )
    }
  }
}