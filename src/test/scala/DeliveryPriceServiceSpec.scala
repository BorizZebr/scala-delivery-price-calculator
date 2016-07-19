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
    with DeliveryPriceService{

  override def config = testConfig
  override val logger = NoLogging
  override var model: PriceModel = (_) => 0

  it should "respond with double value on correct price request" in {
    Get(s"/price/250") ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[PriceInfo] shouldBe PriceInfo(250, 0)
    }
  }

  it should "respond with bad request on incorrect weight format" in {
    Get("/thrash/thrash") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }

    Get("/price/thrash") ~> routes ~> check {
      status shouldBe BadRequest
      responseAs[String].length should be > 0
    }
  }
}
