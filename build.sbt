name := "Exploration"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

mainClass := Some("Initial")

libraryDependencies ++= Seq(
  "org.locationtech.geotrellis" %% "geotrellis-vector" % "1.0.0",
  "org.locationtech.geotrellis" %% "geotrellis-shapefile" % "1.0.0",
  "org.locationtech.geotrellis" %% "geotrellis-spark" % "1.0.0",
  "org.locationtech.geotrellis" %% "geotrellis-proj4" % "1.0.0",
  "com.github.tototoshi" %% "scala-csv" % "1.3.4",
  "org.apache.spark" %% "spark-core" % "2.1.0" % "provided"
)


resolvers ++= Seq[Resolver](
  "LocationTech GeoTrellis Snapshots" at "https://repo.locationtech.org/content/repositories/geotrellis-snapshots",
  "Geotools" at "http://download.osgeo.org/webdav/geotools/"
)

assemblyMergeStrategy in assembly := {
  case PathList("com", "vividsolutions", xs @ _*) => MergeStrategy.first
   case x: String =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

