package com.zebrosoft

import scala.io.Source

/**
  * Created by borisbondarenko on 19.07.16.
  */
trait DeliveryPriceModel {
  type PriceModel = Double => Double
  val model: PriceModel
}

trait StaticallyTrainedLinearModel extends DeliveryPriceModel {

  case class PostPriceRaw(minWeight: Double, maxWeight: Double, price: Double)

  case class PostPrice(minWeight: Double, maxWeight: Double, price: Double, probability: Double)

  type PostGrid = Vector[PostPrice]

  val invalidInterval = -1

  val fixedPoint = (0.0, 70.0)

  // read postPrices
  val postPrices: Vector[PostPriceRaw] = {
    val pricesSource = Source.fromFile(getClass.getClassLoader.getResource("prices.csv").toURI)
    val postPrices = pricesSource.getLines.toArray.map(_.split(",")).map { x =>
      (x(0).toDouble, x(1).toDouble)
    }
    pricesSource.close

    (postPrices zip postPrices.tail).map { case(a, b) =>
      PostPriceRaw(a._1, b._1, b._2)
    }.toVector
  }

  // read packages stats
  val packages: Vector[Double] = {
    val packagesSource = Source.fromFile(getClass.getClassLoader.getResource("packages.csv").toURI)
    val packages = packagesSource.getLines.toArray.map(_.toDouble)
    packagesSource.close
    packages.toVector
  }

  // which post interval weight is belong to
  def belongsToInterval(w: Double): Int =
    postPrices.zipWithIndex.foldLeft(invalidInterval) { case(a, (v, idx)) =>
      if (v.minWeight < w && w < v.maxWeight) idx else a
    }

  // probability of interval
  val postGrid: PostGrid = {
    val overalCount = packages.length
    val grouped = (packages.groupBy(belongsToInterval) - invalidInterval).map { case(k, v) =>
      (k, v.length)
    }.toSeq.sortBy(_._1).map(_._2.toDouble)

    (postPrices zip grouped).map { case(p, v) =>
      PostPrice(p.minWeight, p.maxWeight, p.price, v / overalCount)
    }
  }

  val s = packages.map { x =>
    val int = belongsToInterval(x)
    postGrid(int).price
  }.sum

  val n = packages.length.toDouble

  val sumW = packages.sum

  override val model = {

    val wc = fixedPoint._1
    val pc = fixedPoint._2

    val k = (s - pc * n) / (sumW - n * wc)
    val b = pc - k * wc

    println(s"model: k = $k, b = $b")

    (w: Double) => k * w + b
  }

  val byModel = packages.map(model).sum

  println(s"by post: $s, by model: $byModel")
}