package com.zebrosoft

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import spray.json._
import akka.http.scaladsl.model.StatusCodes._

import scala.concurrent.ExecutionContextExecutor
import scala.io.{Source, StdIn}

/**
  * Created by borisbondarenko on 19.07.16.
  */
trait DeliveryPriceService extends SprayJsonSupport
    with DefaultJsonProtocol {

  type PriceFunction = Double => Double
  type PriceModel = (PriceFunction, PriceFunction)

  implicit val priceInfoFormat = jsonFormat3(PriceInfo)
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  implicit def executor: ExecutionContextExecutor

  def config: Config

  val logger: LoggingAdapter

  val models: Map[String, PriceModel]

  val routes =
    pathPrefix("price" / Segment / DoubleNumber) { (modelName, weight) =>
      get {
        models.get(modelName) match {
          case Some(m) =>
            val mPrice = if (weight > 0) m._1(weight) else 0
            val pPrice = if (weight > 0) m._2(weight) else 0
            complete(PriceInfo(weight, mPrice, pPrice))

          case None => complete(BadRequest, "")
        }
      }
    } ~ path("models") {
      get {
        complete(models.keys)
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

  println(s"Reading models...")
  override val models: Map[String, PriceModel] = {
    import scala.collection.JavaConversions._

    config.getConfigList("postModel").map { conf =>
      val name = conf.getString("name")
      val cConfig = conf.getConfig("fixedPoint")

      val fixedPoint = Point(cConfig.getDouble("w"), cConfig.getDouble("p"))
      val postPrices = conf.getConfigList("prices").map { x =>
        (x.getDouble("w"), x.getDouble("p"))
      }

      val postPriceFunction: Double => Double = w =>
        postPrices.find(w < _._1) match {
          case Some(x) => x._2
          case None => 0.0
        }

      val modelPriceFunction = buildModel(fixedPoint, getPackages)(postPriceFunction)

      name -> (modelPriceFunction, postPriceFunction)
    }.toMap
  }

  val bindingFuture = Http().bindAndHandle(routes, interface, port)

  println(s"Delivery price service is online at http://$interface:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
  println(s"Delivery price service is shut down!")
}