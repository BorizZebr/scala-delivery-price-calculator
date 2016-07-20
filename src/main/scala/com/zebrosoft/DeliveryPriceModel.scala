package com.zebrosoft

/**
  * Created by borisbondarenko on 19.07.16.
  */
trait DeliveryPriceModel {
  type PriceModel = Double => Double
  val model: PriceModel
}

trait LinearModelBuilder {

  val invalidInterval = -1

  def buildModel(
      postPrices: Vector[PostPrice],
      packages: Vector[Double],
      fixedPoint: (Double, Double)): Double => Double = {

    // which post interval weight is belong to
    def postPriceByWeight(w: Double): Double =
      postPrices.foldLeft(0) { case (a, v) =>
        if (v.minWeight < w && w < v.maxWeight) return v.price else a
      }

    val wc = fixedPoint._1
    val pc = fixedPoint._2

    val n = packages.length
    val sumW = packages.sum

    val s = packages.map(postPriceByWeight).sum

    val k = (s - pc * n) / (sumW - n * wc)
    val b = pc - k * wc

    (w: Double) => k * w + b
  }
}