package com.zebrosoft

/**
  * Created by borisbondarenko on 19.07.16.
  */
trait DeliveryPriceModel {
  type PriceModel = Double => Double
  var model: PriceModel
}

trait LinearModelTrainer {

  def trainModel(weights: Seq[Double]) : DeliveryPriceMicroService.PriceModel =
    (_: Double) => 0
}