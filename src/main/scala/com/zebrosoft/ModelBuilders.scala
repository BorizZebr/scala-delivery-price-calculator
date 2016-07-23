package com.zebrosoft

/**
  * Created by borisbondarenko on 23.07.16.
  */
trait ModelBuilder extends CalcModelBuilder with PostModelBuilder

trait CalcModelBuilder {

  def buildCalcModel(
      f: Point,
      packages: Vector[Double])
      (postPrice: Double => Double): Double => Double = {

    val n = packages.length
    val sumW = packages.sum

    val s = packages.map(postPrice).sum

    val k = (s - f.p * n) / (sumW - n * f.w)
    val b = f.p - k * f.w

    (w: Double) => k * w + b
  }
}

trait PostModelBuilder {

  def buildPostModel(postPrices: Vector[Point]): Double => Double = { w =>
    postPrices.find(w < _.w) match {
      case Some(x) => x.p
      case None => 0.0
    }
  }
}