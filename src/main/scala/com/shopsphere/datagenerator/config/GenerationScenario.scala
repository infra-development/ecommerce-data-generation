package com.shopsphere.datagenerator.config

sealed trait GenerationScenario {
  def name: String
}

object GenerationScenario {

  case object Baseline extends GenerationScenario {
    override val name: String = "baseline"
  }

  val all: Seq[GenerationScenario] = Seq(
    Baseline
  )

  def fromString(value: String): Either[String, GenerationScenario] = {
    all.find(_.name == value) match {
      case Some(scenario) => Right(scenario)
      case None =>
        Left(
          s"Unsupported generation scenario: '$value'. " +
            s"Supported scenarios: ${all.map(_.name).sorted.mkString(", ")}"
        )
    }
  }
}