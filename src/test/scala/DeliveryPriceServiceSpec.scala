import akka.event.NoLogging
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.zebrosoft.{DeliveryPriceService, PriceInfo}
import org.scalatest.{FlatSpec, Matchers}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ContentTypes._

/**
  * Created by borisbondarenko on 19.07.16.
  */
class DeliveryPriceServiceSpec extends FlatSpec
    with Matchers
    with ScalatestRouteTest
    with DeliveryPriceService {

  override def config = testConfig
  override val logger = NoLogging

  override val models: Map[String, (PriceFunction, PriceFunction)] = Map(
    "AZAZA" -> ((x: Double) => x * 2, (x: Double) => x * 4))

  it should "respond with correct value on correct price request" in {
    Get(s"/price/AZAZA/250") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PriceInfo] shouldBe PriceInfo(250.0, 500.0, 1000.0)
    }
  }

  it should "respond with zero value on zero or less then zero weight" in {
    Get(s"/price/AZAZA/-250") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PriceInfo] shouldBe PriceInfo(-250.0, 0.0, 0.0)
    }
  }
}
