package com.zebrosoft

/**
  * Created by borisbondarenko on 23.07.16.
  */
trait ModelsBuilder {

  def buildModels(configs: Map[String, PostConfig], packages: Vector[Double]): Map[String, PriceModel] =
    configs.map { case(name, PostConfig(fixedPoint, postPrices)) =>
      val postPriceFunction = buildPostModel(postPrices)
      val modelPriceFunction = buildCalcModel(fixedPoint, packages)(postPriceFunction)
      name -> PriceModel(modelPriceFunction, postPriceFunction)
    }

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

  def buildPostModel(postPrices: Vector[Point]): Double => Double = { w =>
    postPrices.find(w < _.w) match {
      case Some(x) => x.p
      case None => postPrices.last.p
    }
  }
}