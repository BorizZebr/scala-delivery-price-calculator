package com.zebrosoft

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.io.{Source, StdIn}

/**
  * Created by borisbondarenko on 19.07.16.
  */
trait DeliveryPriceService extends SprayJsonSupport
    with DefaultJsonProtocol {

  type PriceModel = Double => Double

  implicit val priceInfoFormat = jsonFormat2(PriceInfo)
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit def executor: ExecutionContextExecutor

  def config: Config
  val logger: LoggingAdapter

  val model: PriceModel

  val routes =
    pathPrefix("price" / DoubleNumber) { weight =>
      get {
        val result = if (weight > 0) model(weight) else 0
        complete(PriceInfo(weight, result))
      }
    }
}

trait PackageStatsPersister {

  val pathPack: String

  def getPackages: Vector[Double] = {
    val packagesSource = Source.fromFile(getClass.getClassLoader.getResource(pathPack).toURI)
    val packages = packagesSource.getLines.toArray.map(_.toDouble)
    packagesSource.close
    packages.toVector
  }

  def persistPackage(w: Double): Unit = {

  }
}

trait ModelBuilder {

  def buildModel(
      postModel: PostModel,
      packages: Vector[Double]): Double => Double = {

    val n = packages.length
    val sumW = packages.sum

    val s = packages.map(postModel.deliveryPrice).sum
    val f = postModel.fixedPoint

    val k = (s - f.p * n) / (sumW - n * f.w)
    val b = f.p - k * f.w

    (w: Double) => k * w + b
  }
}

object DeliveryPriceMicroService extends App
  with DeliveryPriceService
  with ModelBuilder
  with PackageStatsPersister {

  override implicit val system = ActorSystem("delivery-price-system")
  override implicit val materializer = ActorMaterializer()
  override implicit val executor = system.dispatcher

  override val pathPack: String = "packages.csv"
  override val logger = Logging(system, getClass)
  override val config = ConfigFactory.load()
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  // read postPrices
  val postModel: PostModel = {

    import scala.collection.JavaConversions._

    val cConfig = config.getConfig("postModel.fixedPoint")
    val postPrices = config.getConfigList("postModel.prices").map { x =>
      (x.getDouble("w"), x.getDouble("p"))
    }

    PostModel(
      Point(cConfig.getDouble("w"), cConfig.getDouble("p")),
      (postPrices zip postPrices.tail).map { case(a, b) =>
        PostPrice(a._1, b._1, b._2)
      }.toVector)
  }

  override val model: PriceModel = buildModel(postModel, getPackages)

  val bindingFuture = Http().bindAndHandle(routes, interface, port)

  println(s"Delivery price service is online at http://$interface:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}