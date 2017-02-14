import geotrellis.shapefile.ShapeFileReader
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.rasterize._

import java.io.File
import com.github.tototoshi.csv._
import geotrellis.raster.resample.{ResampleMethod, NearestNeighbor}
import geotrellis.raster.mapalgebra._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

import geotrellis.proj4._
import geotrellis.spark._
import geotrellis.spark.tiling._


object Initial {
  def main(args: Array[String]) {

    if (args.length != 2) {
      System.err.println(s"""
        |Usage: Indexer <mobile>
        |  <mobile> is a path to mobile shape file
        |  <out> is a path for the output file
        """.stripMargin)
      System.exit(1)
    }

    val Array(input, out) = args

    val econ = ShapeFileReader.readMultiPolygonFeatures("../MESO/meso_2010_econ_dd/meso_2010_econ_dd.shp")

    val mobile = ShapeFileReader.readMultiPolygonFeatures(input)


    println("combining mobile data.")

    // we throw out metadata for mobile, then union all the pieces together
    val mobileCombined = mobile.map(_.geom).reduce(_.union(_).asMultiPolygon.get)


    println("Intersecting Area and Creating Covered")
    val covered = econ
      .map(_ & mobileCombined)
      .map {
      case MultiPolygonResult(x) => x
      case PolygonResult(x) => MultiPolygon(x)
      case NoResult => MultiPolygon()
      }
      .map(_.area)
      .zip(econ.map(_.area))
      .map(a => Map(("covered_area" -> a._1), ("covered_percentage" -> a._1/a._2)))


    println("Generating Covered Area")
    val dat = econ.zip(covered).map(a => a._1.mapData(d => d ++ a._2))

    val lilEcon = dat.filter(_.data.get("covered_area") match { case Some(x:Double) => x > 0.0 case _ => false })

    // println("Writing to csv: ")
    // val writer = CSVWriter.open(new File(out))
    // writer.writeRow(dat(0).keys.toSeq)
    // writer.writeAll(dat.map(_.toSeq.map(_._2)))
    // writer.close()

    val conf = new SparkConf().setAppName("Initial")
    val sc = new SparkContext(conf)

    val mobilesR = mobile.map(m => vector2raster(m, 1000, 1000))
    val mobilesRDD = sc.parallelize(mobilesR)

    val crs = CRS.fromEpsgCode(4326)
    val bounds = KeyBounds(SpatialKey(1,1), SpatialKey(10, 10))
    val extent = mobileCombined.envelope
    val layout = new LayoutDefinition(extent, new TileLayout(layoutCols= 10, layoutRows = 10, tileCols = 1000, tileRows = 1000))
    val cellType = IntConstantNoDataCellType
    val layerMetadata = new TileLayerMetadata[SpatialKey](cellType, layout, extent, crs, bounds)

  }


  def vector2raster(vec: Geometry, cols:Int, rows:Int, value:Int = 1) : Raster[Tile] =  {
    val re = RasterExtent(vec.envelope, cols = cols, rows = rows)
    val tile = Rasterizer.rasterizeWithValue(vec, re, value)
    Raster(tile, vec.envelope)
  }


  def intersectRasters(r1: Raster[Tile], r2: Raster[Tile], method:ResampleMethod = NearestNeighbor) = {
    val resampled = r1.resample(r2.rasterExtent, method = method)
    Raster(local.And(resampled, r2), resampled.extent)
  }



  val write = (r:Raster[Tile], f:String) => r._1.map(i => if(i > 0) 0xFF0000FF else 0x00000000).renderPng.write(f)


  // val t = sc.textFile("../data/2009/Data/3G/Global_3G_2009Q1.prj")
  // val prjString = t.take(1)(0)
m
  // val lilEconR = vector2raster(lilEcon(1), 1000, 1000)
  // import geotrellis.raster.render._

}
