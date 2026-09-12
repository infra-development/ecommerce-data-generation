package com.shopsphere.datagenerator.validation

case class ValidationIssue(
                            category: String,
                            entity: String,
                            message: String
                          )

case class ValidationResult(
                             issues: Seq[ValidationIssue]
                           ) {

  def isValid: Boolean =
    issues.isEmpty

  def add(
           category: String,
           entity: String,
           message: String
         ): ValidationResult = {

    copy(
      issues =
        issues :+
          ValidationIssue(
            category = category,
            entity = entity,
            message = message
          )
    )
  }
}