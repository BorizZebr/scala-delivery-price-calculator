package com.zebrosoft

import scala.io.Source

/**
  * Created by borisbondarenko on 23.07.16.
  */
trait PackagesRepo {

  def getPackages: Vector[Double]
  def storePackage(w: Double): Unit
}

trait CsvPackagesRepo extends PackagesRepo {

  val pathPack: String

  def getPackages: Vector[Double] = {
    val packagesSource = Source.fromFile(getClass.getClassLoader.getResource(pathPack).toURI)
    val packages = packagesSource.getLines.toArray.map(_.toDouble)
    packagesSource.close
    packages.toVector
  }

  def storePackage(w: Double): Unit = {

  }
}
