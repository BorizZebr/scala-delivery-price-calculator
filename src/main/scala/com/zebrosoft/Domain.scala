package com.zebrosoft

/**
  * Created by borisbondarenko on 20.07.16.
  */

case class Point(w: Double, p: Double)

case class PriceInfo(weight: Double, price: Double)

case class PostPrice(minWeight: Double, maxWeight: Double, price: Double)

case class PostModel(fixedPoint: Point, prices: Vector[PostPrice]) {
  def deliveryPrice(w: Double): Double =
    prices.foldLeft(0) { case (a, v) =>
      if (v.minWeight < w && w < v.maxWeight) return v.price else a
    }
}
