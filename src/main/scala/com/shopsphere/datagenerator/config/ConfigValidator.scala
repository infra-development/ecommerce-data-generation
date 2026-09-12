package com.shopsphere.datagenerator.config

object ConfigValidator {

  def validate(config: GenerationConfig): Unit = {
    validateOutputDirectory(config.output.directory)
  }

  private def validateOutputDirectory(directory: String): Unit = {
    if (directory.trim.isEmpty) {
      throw new IllegalArgumentException(
        "Output directory must not be empty."
      )
    }
  }
}