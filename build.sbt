
scalaVersion := "2.11.7"

libraryDependencies ++= Seq("javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
            "org.scala-lang.modules" %% "scala-xml" % "1.0.2")


enablePlugins(JettyPlugin)

containerPort := 9090

containerForkOptions := new ForkOptions(runJVMOptions = Seq("-Dfoo=bar"))
javaOptions in Jetty ++= Seq(
  "-Xdebug",
  "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
)
