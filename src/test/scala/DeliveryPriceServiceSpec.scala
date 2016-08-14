import akka.event.NoLogging
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.zebrosoft._
import org.scalatest.{FlatSpec, Matchers}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ContentTypes._

/**
  * Created by borisbondarenko on 19.07.16.
  */
class DeliveryPriceServiceSpec extends FlatSpec
    with Matchers
    with ScalatestRouteTest
    with TestModelsBuilder
    with TestPackagesRepo
    with DeliveryPriceService {

  override def config = testConfig
  override val logger = NoLogging

  it should "initialize models on bootstrap" in {
    models shouldBe testModels
  }

  it should "initialize post models on bootstrap" in {
    postConfigs should contain key "first"
    postConfigs should contain key "second"
  }

  it should "respond with correct value on correct price request" in {
    Get("/price/AZAZA/250") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PriceResponse] shouldBe PriceResponse(250.0, 500.0, 1000.0)
    }
  }

  it should "respond with zero value on zero or less then zero weight" in {
    Get("/price/AZAZA/-250") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PriceResponse] shouldBe PriceResponse(-250.0, 0.0, 0.0)
    }
  }

  it should "respond with BadRequest status in case of not existing model" in {
    Get("/price/AKAKA/100500") ~> routes ~> check {
      status shouldBe BadRequest
    }
  }

  it should "return names of models that it has" in {
    Get("/models") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[Seq[String]] shouldBe Seq("AZAZA", "OLOLO")
    }
  }

  it should "store packages" in {
    // Arrange
    val testPackages = Seq(Package(1.0), Package(2.0), Package(3.0))
    // Act
    testPackages.foreach {
      Post("/package", _) ~> routes ~> check {
        status shouldBe OK
      }
    }
    // Assert
    testPackages shouldBe packagesStorage
  }
}

trait TestModelsBuilder extends ModelsBuilder {

  val testModels = Map(
    "AZAZA" -> PriceModel((x: Double) => x * 2, (x: Double) => x * 4),
    "OLOLO" -> PriceModel(_ => 100500, (x: Double) => x / 2))

  override def buildModels(configs: Map[String, PostConfig], packages: Vector[Double]) = testModels
}

trait TestPackagesRepo extends PackagesRepo {

  var packagesStorage: Seq[Package] = Seq.empty

  override def getPackages: Vector[Double] =
    packagesStorage.map(_.weight).toVector

  override def storePackage(w: Double): Unit =
    packagesStorage = packagesStorage :+ Package(w)
}
