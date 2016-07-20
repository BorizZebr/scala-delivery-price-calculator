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
import scala.io.StdIn

/**
  * Created by borisbondarenko on 19.07.16.
  */
trait DeliveryPriceService extends DeliveryPriceModel
    with SprayJsonSupport
    with DefaultJsonProtocol {

  implicit val priceInfoFormat = jsonFormat2(PriceInfo)
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit def executor: ExecutionContextExecutor

  def config: Config
  val logger: LoggingAdapter

  val routes =
    pathPrefix("price" / DoubleNumber) { weight =>
      get {
        complete(PriceInfo(weight, model(weight)))
      }
    }
}

object DeliveryPriceMicroService extends App
    with DeliveryPriceService
    with LinearModelBuilder {

  override implicit val system = ActorSystem("delivery-price-system")
  override implicit val materializer = ActorMaterializer()
  override implicit val executor = system.dispatcher

  override val config = ConfigFactory.load()
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  override val logger = Logging(system, getClass)

  override val model: PriceModel = {

    import scala.io.Source

    // read postPrices
    val postPrices: Vector[PostPrice] = {
      val pricesSource = Source.fromFile(getClass.getClassLoader.getResource("prices.csv").toURI)
      val postPrices = pricesSource.getLines.toArray.map(_.split(",")).map { x =>
        (x(0).toDouble, x(1).toDouble)
      }
      pricesSource.close

      (postPrices zip postPrices.tail).map { case(a, b) =>
        PostPrice(a._1, b._1, b._2)
      }.toVector
    }

    // read packages stats
    val packages: Vector[Double] = {
      val packagesSource = Source.fromFile(getClass.getClassLoader.getResource("packages.csv").toURI)
      val packages = packagesSource.getLines.toArray.map(_.toDouble)
      packagesSource.close
      packages.toVector
    }

    // fixed point
    val fixedPoint = (config.getDouble("service.fixedPoint.weight"), config.getDouble("service.fixedPoint.price"))

    buildModel(postPrices, packages, fixedPoint)
  }

  val bindingFuture = Http().bindAndHandle(routes, interface, port)

  println(s"Delivery price microservice is online at http://$interface:$port/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
