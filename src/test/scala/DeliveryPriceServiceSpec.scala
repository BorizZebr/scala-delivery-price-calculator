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
    with DeliveryPriceService
    with FakeModelBuilder
    with FakePackagesRepo {

  override def config = testConfig
  override val logger = NoLogging
  override val postConfigs: Map[String, PostConfig] = Map.empty

  it should "respond with correct value on correct price request" in {
    Get(s"/price/AZAZA/250") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PriceResponse] shouldBe PriceResponse(250.0, 500.0, 1000.0)
    }
  }

  it should "respond with zero value on zero or less then zero weight" in {
    Get(s"/price/AZAZA/-250") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PriceResponse] shouldBe PriceResponse(-250.0, 0.0, 0.0)
    }
  }
}

trait FakeModelBuilder extends ModelsBuilder {

  override def rebuildModels(
      configs: Map[String, PostConfig],
      packages: Vector[Double]): Map[String, PriceModel] = Map(
    "AZAZA" -> PriceModel((x: Double) => x * 2, (x: Double) => x * 4),
    "OLOLO" -> PriceModel(_ => 100500, (x: Double) => x / 2))
}

trait FakePackagesRepo extends PackagesRepo {

  override def getPackages: Vector[Double] = Vector.empty
  override def storePackage(w: Double): Unit = ()
}
