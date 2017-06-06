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
    with ModelsBuilder
    with PackagesRepo {

  implicit val packageFormat = jsonFormat1(Package)
  implicit val priceResponseFormat = jsonFormat3(PriceResponse)
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit def executor: ExecutionContextExecutor

  val logger: LoggingAdapter
  def config: Config

  val postConfigs: Map[String, PostConfig] = {
    import scala.collection.JavaConversions._

    config.getConfigList("postModel").map { conf =>
      val name = conf.getString("name")
      val cConfig = conf.getConfig("fixedPoint")

      val fixedPoint = Point(cConfig.getDouble("w"), cConfig.getDouble("p"))
      val postPrices = conf.getConfigList("prices").map { x =>
        Point(x.getDouble("w"), x.getDouble("p"))
      }.toVector

      name -> PostConfig(fixedPoint, postPrices)
    }.toMap
  }

  var models: Map[String, PriceModel] = buildModels(postConfigs, getPackages)

  def storePackageAndRebuildModels(pckg: Package): Unit = Future {
    storePackage(pckg.weight)
    models = buildModels(postConfigs, getPackages)
  } onFailure {
    case t => logger.error("ALARM!!! Trash in packages!! " + t.getMessage)
  }

  val routes =
    pathPrefix("price" / Segment / DoubleNumber) { (modelName, weight) =>
      get {
        models.get(modelName) match {
          case Some(m) =>
            val mPrice = if (weight > 0) m.calcModel(weight) else 0
            val pPrice = if (weight > 0) m.postModel(weight) else 0
            complete(PriceResponse(weight, mPrice, pPrice))

          case None => complete(BadRequest, "There's no such model!")
        }
      }
    } ~ pathPrefix("package") {
      post {
        entity(as[Package]) { pckg =>
          storePackageAndRebuildModels(pckg)
          complete(OK, s"$pckg - ok!")
        }
      }
    } ~ path("models") {
      get {
        complete(models.keys)
      }
    }
}

object DeliveryPriceMicroService extends App
    with DeliveryPriceService {

  override implicit val system = ActorSystem("delivery-price-system")
  override implicit val materializer = ActorMaterializer()
  override implicit val executor = system.dispatcher

  override val logger = Logging(system, getClass)
  override def config = ConfigFactory.load()

  //println(s"Reading models...")
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")
  //val bindingFuture =
  Http().bindAndHandle(routes, interface, port)
  //println(s"Delivery price service is online at http://$interface:$port/\nPress RETURN to stop...")

//  StdIn.readLine()
//  bindingFuture
//    .flatMap(_.unbind())
//    .onComplete(_ => system.terminate())
//
//  println(s"Delivery price service is shut down!")
}