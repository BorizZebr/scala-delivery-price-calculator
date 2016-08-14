package com.zebrosoft

import scala.io.Source
import java.nio.file.{Files, Paths, StandardOpenOption}

/**
  * Created by borisbondarenko on 23.07.16.
  */
trait PackagesRepo {

  val pathPack: String = "packages.csv"

  def getPackages: Vector[Double] =
    Source.fromFile(getClass.getClassLoader.getResource(pathPack).toURI)
      .getLines
      .map(_.toDouble)
      .toVector

  def storePackage(w: Double): Unit = {
    val path = Paths.get(getClass.getClassLoader.getResource(pathPack).toURI)
    Files.write(path, s"\n$w".getBytes, StandardOpenOption.APPEND)
  }
}
