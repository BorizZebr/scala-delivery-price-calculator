package com.zebrosoft

/**
  * Created by borisbondarenko on 20.07.16.
  */

case class Point(w: Double, p: Double)

case class PriceInfo(weight: Double, priceByCalc: Double, priceByPost: Double)

case class PriceModel(calcModel: Double => Double, postModel: Double => Double)
