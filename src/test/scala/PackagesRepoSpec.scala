import java.nio.file.{Files, Paths}

import com.zebrosoft.PackagesRepo
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
  * Created by borisbondarenko on 24.07.16.
  */
class PackagesRepoSpec extends FlatSpec
  with PackagesRepo
  with Matchers
  with BeforeAndAfter {

  val sourcePathPack: String = "source.csv"
  override val pathPack: String = "testPackages.cvs"

  before {
    val path = Paths.get(getClass.getClassLoader.getResource(sourcePathPack).toURI)
    Files.copy(path, Paths.get(path.getParent.toString, pathPack))
  }

  after {
    val path = Paths.get(getClass.getClassLoader.getResource(pathPack).toURI)
    Files.delete(path)
  }

  it should "read packages" in {
    val packages = getPackages
    assert(packages.length === 5)
  }

  it should "write packages" in {
    storePackage(100500.0)
    val packages = getPackages

    assert(packages.length === 6)
    assert(packages.last === 100500.0)
  }
}
