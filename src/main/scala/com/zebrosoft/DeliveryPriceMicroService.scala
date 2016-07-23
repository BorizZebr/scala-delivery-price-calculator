package com.zebrosoft

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import spray.json._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

/**
  * Created by borisbondarenko on 19.07.16.
  */
trait DeliveryPriceService extends SprayJsonSupport
    with DefaultJsonProtocol
    with ModelBuilder
    with PackagesRepo {

  implicit val priceInfoFormat = jsonFormat3(PriceInfo)
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit def executor: ExecutionContextExecutor

  val logger: LoggingAdapter
  def config: Config
  def rebuildModels: Map[String, PriceModel]

  var models: Map[String, PriceModel] = rebuildModels

  val routes =
    pathPrefix("price" / Segment / DoubleNumber) { (modelName, weight) =>
      get {
        models.get(modelName) match {
          case Some(m) =>
            val mPrice = if (weight > 0) m.calcModel(weight) else 0
            val pPrice = if (weight > 0) m.postModel(weight) else 0
            complete(PriceInfo(weight, mPrice, pPrice))

          case None => complete(BadRequest, "There's no such model!")
        }
      }
    } ~ pathPrefix("package" / DoubleNumber) { weight =>
      post {
        Future {
          storePackage(weight)
          models = rebuildModels
        }
        complete(OK)
      }
    } ~ path("models") {
      get {
        complete(models.keys)
      }
    }
}

object DeliveryPriceMicroService extends App
    with DeliveryPriceService
    with CsvPackagesRepo {

  override implicit val system = ActorSystem("delivery-price-system")
  override implicit val materializer = ActorMaterializer()
  override implicit val executor = system.dispatcher

  override val pathPack: String = "packages.csv"
  override val logger = Logging(system, getClass)
  override val config = ConfigFactory.load()
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  override def rebuildModels: Map[String, PriceModel] = {
    import scala.collection.JavaConversions._

    config.getConfigList("postModel").map { conf =>
      val name = conf.getString("name")
      val cConfig = conf.getConfig("fixedPoint")

      val fixedPoint = Point(cConfig.getDouble("w"), cConfig.getDouble("p"))
      val postPrices = conf.getConfigList("prices").map { x =>
        Point(x.getDouble("w"), x.getDouble("p"))
      }.toVector

      // building models for "name" post
      val postPriceFunction = buildPostModel(postPrices)
      val modelPriceFunction = buildCalcModel(fixedPoint, getPackages)(postPriceFunction)
      name -> PriceModel(modelPriceFunction, postPriceFunction)
    }.toMap
  }

  println(s"Reading models...")

  val bindingFuture = Http().bindAndHandle(routes, interface, port)

  println(s"Delivery price service is online at http://$interface:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())

  println(s"Delivery price service is shut down!")
}