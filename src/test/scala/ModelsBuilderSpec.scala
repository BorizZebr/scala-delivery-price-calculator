import com.zebrosoft._
import org.scalatest.{FunSpec, Matchers}

import scala.util.Random

/**
  * Created by borisbondarenko on 207.16.
  */
class ModelsBuilderSpec extends FunSpec
  with ModelsBuilder
  with Matchers {

  describe("A flat calc model") {

    val packages: Vector[Double] = Vector(5, 15, 25)
    val model = buildCalcModel(Point(0, 1), packages)(_ => 1.0)

    it("should have a result 1 on 0 weight") {
      assert(model(0) === 1)
    }

    it("should have a result 1 on -10 weight") {
      assert(model(-10) === 1)
    }

    it("should have a result 1 on 15 weight") {
      assert(model(15) === 1)
    }

    it("should have a result 1 on 50 weight") {
      assert(model(50) === 1)
    }

    it("should not loose our money:)") {
      assert(packages.map(model).sum === 3)
    }
  }

  describe("A non-flat calc model") {

    val postPrice: Double => Double = x => x * 10.0 - 50

    val packages: Vector[Double] = {
      val rnd = new Random()
      Vector.fill(100)(rnd.nextDouble * 1000)
    }

    val model = buildCalcModel(Point(0, 50), packages)(postPrice)

    it("should be 50 in 0 weight (fixed point)") {
      assert(model(0.0) === 50.0)
    }

    it("should not loose our money:) -- difference between model and post less then 1%") {
      val sumByModel = packages.map(model).sum
      val sumByPost = packages.map(postPrice).sum
      assert(1.0 - sumByModel / sumByPost < 0.01, sumByModel)
    }
  }

  describe("Post model with only one interval") {

    val model = buildPostModel(Vector(Point(1000.0, 100.0)))

    val testWeights = Seq(-10.0, 0.0, 1.0, 10.0, 100.0, 500.0, 1000.0, 5000.0)

    it("should return constant price") {
      assert(testWeights.map(model).forall(_ == 100.0))
    }
  }

  describe("Post model with several intervals") {

    val model = buildPostModel(Vector(Point(100.0, 10.0), Point(200.0, 30.0), Point(300.0, 50.0)))

    it("should return first interval price for negative weight") {
      assert(model(-1.0) === 10.0)
    }

    it("should return first interval price for zero weight") {
      assert(model(0.0) === 10.0)
    }

    it("should return second interval price for pivot between first/second") {
      assert(model(100.0) === 30.0)
    }

    it("should return third interval price for pivot between second/third") {
      assert(model(200.0) === 50.0)
    }
  }

  describe("Rebuild models") {

    val packages = Vector(1.0, 2.0, 3.0, 4.0, 5.0)
    val configs = Map(
      "first" -> PostConfig(Point(0.0, 0.0), Vector(Point(10.0, 10.0))),
      "second" -> PostConfig(Point(0.0, 0.0), Vector(Point(10.0, 10.0)))
    )
    val models = buildModels(configs, packages)

    it("should produce map of models") {
      assert(models.contains("first"))
      assert(models.contains("second"))
    }
  }
}
