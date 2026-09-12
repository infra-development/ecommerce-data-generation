package com.shopsphere.datagenerator.generation

object CardinalityAllocator {

  /**
   * Allocates an exact total count across a number of base records
   * while keeping the per-record counts as close as possible to the
   * configured average.
   *
   * Example:
   *
   *   baseCount = 5000
   *   average   = 2.4
   *
   * produces:
   *
   *   3000 records with 2 items
   *   2000 records with 3 items
   *
   * Total = 12000
   */
  def allocate(
                baseCount: Long,
                averagePerBase: Double
              ): Seq[Int] = {

    require(
      baseCount >= 0,
      "Base count must not be negative."
    )

    require(
      !averagePerBase.isNaN &&
        !averagePerBase.isInfinity &&
        averagePerBase >= 0.0,
      "Average cardinality must be finite and must not be negative."
    )

    if (baseCount == 0) {
      Seq.empty
    } else if (averagePerBase == 0.0) {
      Seq.fill(baseCount.toInt)(0)
    } else {

      val totalCount =
        Math.round(
          baseCount.toDouble * averagePerBase
        )

      val lowerCount =
        Math.floor(averagePerBase).toInt

      val remainder =
        totalCount -
          baseCount * lowerCount

      val higherCount =
        lowerCount + 1

      val higherRecords =
        remainder.toInt

      val lowerRecords =
        baseCount.toInt - higherRecords

      Seq.fill(lowerRecords)(lowerCount) ++
        Seq.fill(higherRecords)(higherCount)
    }
  }
}