package com.zebrosoft

/**
  * Created by borisbondarenko on 20.07.16.
  */

case class Point(w: Double, p: Double)

case class PostConfig(fixedPoint: Point, priceGrid: Vector[Point])

case class PriceModel(calcModel: Double => Double, postModel: Double => Double)

case class Package(weight: Double)

case class PriceResponse(weight: Double, priceByCalc: Double, priceByPost: Double)
