import com.zebrosoft._
import org.scalatest.{FlatSpec, FunSpec, Matchers}

import scala.util.Random

/**
  * Created by borisbondarenko on 207.16.
  */
class ModelBuilderSpec extends FunSpec
  with ModelBuilder
  with Matchers {

  describe("A flat model") {

    val postModel = PostModel(
      Point(0, 1),
      Vector(
        PostPrice(0, 10, 1),
        PostPrice(10, 20, 1),
        PostPrice(20, 30, 1)))

    val packages: Vector[Double] = Vector(5, 15, 25)

    val model = buildModel(postModel, packages)

    it("should have a result 1 on 0 weight") {
      assert(model(0) == 1)
    }

    it("should have a result 1 on -10 weight") {
      assert(model(-10) == 1)
    }

    it("should have a result 1 on 15 weight") {
      assert(model(15) == 1)
    }

    it("should have a result 1 on 50 weight") {
      assert(model(50) == 1)
    }

    it("should not loose our money:)") {
      assert(packages.map(model).sum == 3)
    }
  }

  describe("A non-flat model") {

    val postModel = PostModel(
      Point(0, 50),
      Vector(
        PostPrice(0, 100, 60),
        PostPrice(100, 200, 100),
        PostPrice(200, 1000, 300)))

    val packages: Vector[Double] = {
      val rnd = new Random()
      Vector.fill(100)(rnd.nextDouble * 1000)
    }

    val model = buildModel(postModel, packages)

    it("should be 50 in 0 weight (fixed point)") {
      assert(model(0) == 50)
    }

    it("should not loose our money:) -- difference between model and post less then 1%") {
      val sumByModel = packages.map(model).sum
      val sumByPost = packages.map(postModel.deliveryPrice).sum
      assert(1.0 - sumByModel / sumByPost < 0.01, sumByModel)
    }
  }
}
