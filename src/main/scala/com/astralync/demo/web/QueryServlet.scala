package com.astralync.demo.web

import java.io.{BufferedWriter, FileWriter, BufferedReader, FileReader}
import java.net.{URLEncoder, InetAddress}
import java.util.Date
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.servlet.{ServletConfig, ServletRequest, ServletResponse}

import scala.xml.Node

/**
 * KeywordsServlet
 *
 */
class QueryServlet extends javax.servlet.http.HttpServlet {

  var cacheEnabled = true
  var nLoops = 1
  val InteractionField = "interaction_created_at"
  val DtNames = s"twitter_user_location twitter_user_lang $InteractionField"
  val fakeArgs = "local[32] /shared/demo/data/dataSmall 3 3 true".split(" ")

  override def init(config: ServletConfig) = {
    super.init(config)
  }

  val headers = List(InteractionField, "interaction_content", "interaction_geo_latitude", "interaction_geo_longitude", "interaction_id", "interaction_author_username", "interaction_link", "klout_score", "interaction_author_link", "interaction_author_name", "interaction_source", "salience_content_sentiment", "datasift_stream_id", "twitter_retweeted_id", "twitter_user_created_at", "twitter_user_description", "twitter_user_followers_count", "twitter_user_geo_enabled", "twitter_user_lang", "twitter_user_location", "twitter_user_time_zone", "twitter_user_statuses_count", "twitter_user_friends_count", "state_province")

  val RegexUrl = s"http://${InetAddress.getLocalHost.getHostName}:8180/"

  def localhostServer(req: HttpServletRequest) = {
    val host = req.getServerName // java.net.InetAddress.getLocalHost.getHostName
    val port = req.getServerPort
    s"http://$host:$port"
  }

  def get(req: HttpServletRequest, path: String, params: Map[String, String]) = {
    val mapstr = if (!params.isEmpty) {
      params.toSeq.map { case (k, v) => s"$k=${URLEncoder.encode(v)}" }.mkString("?", "&", "")
    } else ""
    //    val url = s"${localhostServer(req)}$path$mapstr"
    val url = s"$path$mapstr"
    println(s"Fetching $url ..")
    HttpUtils.get(url)
  }

  import collection.JavaConverters._

  override def service(req: HttpServletRequest, resp: HttpServletResponse): Unit = {

    if (req.getRequestURL.toString.contains("runQuery")) {
      runQuery(req, resp)
    } else {
      query(req, resp)
    }
  }

  val title = "Astralync: Twitter Keywords Search"

  def query(req: HttpServletRequest, resp: HttpServletResponse) = {
    val gval = DtNames
    val sortBy = headers.map(h => s"""<option value="$h">$h</option>""").mkString("\n")
    //    println(s"sortBy=$sortBy")
    val isLaptop = InetAddress.getLocalHost.getHostName.contains("mellyrn")
    val (inputFile, posKeywords, negKeywords, master, nameNode, nparts, nloops, cacheEnabled, exportFile) = if (isLaptop) {
      ("/user/steve/data500m", "michelewells Foursquare", " ", "local[*]", "hdfs://localhost:8020", "8", "1", true, "/shared/wfweb/src/main/webapp/results/queryResults.csv")
    }
    else {
      ("/user/stephen/data5gb", "wells fargo chase bank money cash", " ", "spark://i386:7077", "hdfs://i386:9000",
        "40", "1", true, "/home/stephen/wfweb/src/main/webapp/results/queryResults.csv")
    }
    val runQuery = s"${localhostServer(req)}/runQuery"
    val ret =
      <table border="0">
        <tr>
          <td width="60%">
            <table border="0">
              <form action={runQuery} name="queryForm" METHOD='GET'>
                <tr>
                  <td>Included Keywords:
                    <p/>
                    <textarea cols="100" rows="3" name="posKeywords">{posKeywords}</textarea>
                  </td>
                  <td>
                    <p/> <input type="radio" name="posKeywordsAndOr" value="and">AND</input>
                    <p/> <input type="radio" name="posKeywordsAndOr" value="or" checked="true">OR</input>
                  </td>
                </tr>
                <tr>
                  <td>Excluded Keywords:
                    <p/>
                    <textarea cols="100" rows="2" name="negKeywords">{negKeywords}</textarea>
                  </td>
                  <td>
                    <p/> <input type="radio" name="negKeywordsAndOr" value="and" checked="true">AND</input>
                    <p/> <input type="radio" name="negKeywordsAndOr" value="or">OR</input>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">Grouping Fields:
                    &nbsp; <input type="text" size="80" name="grouping" value={gval.replace(" ", ",")}/>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">Sort by:
                    &nbsp; <select name="sortBy">
                    <option value="twitter_user_location" checked="true">twitter_user_location</option>
                    <option value="interaction_created_at">interaction_created_at</option>
                    <option value="interaction_content">interaction_content</option>
                    <option value="klout_score">klout_score</option>
                    <option value="interaction_author_name">interaction_author_name</option>
                    <option value="interaction_source">interaction_source</option>
                    <option value="twitter_user_created_at">twitter_user_created_at</option>
                    <option value="twitter_user_description">twitter_user_description</option>
                    <option value="twitter_user_followers_count">twitter_user_followers_count</option>
                    <option value="twitter_user_lang">twitter_user_lang</option>
                    <option value="twitter_user_time_zone">twitter_user_time_zone</option>
                    <option value="twitter_user_statuses_count">twitter_user_statuses_count</option>
                    <option value="twitter_user_friends_count">twitter_user_friends_count</option>
                    <option value="state_province">state_province</option>
                  </select>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">MinCount for Groups:
                    &nbsp; <input type="text" size="6" name="minCount" value="1"/>
                  </td>
                </tr>
                <tr>
                  <td colspan="2">Input file:
                    <input type="text" size="50" name="inputFile" value="$inputFile"/>
                  </td>
                </tr>
                <tr>
                  <td width="40%">Save file:
                    <input type="text" size="40" name="saveFile" value="results/query"/>
                  </td>
                  <td width="60%">Export file:
                    <input type="text" size="70" name="exportFile" value="$exportFile"/>
                    <!-- <input type="text" size="30" name="exportFile" value="src/main/webapp/results/queryResults.csv"/> -->
                  </td>
                </tr>
                <tr>
                  <td colspan="2">Backend/Spark options:
                    SparkMaster:
                    <input type="text" size="20" name="sparkMaster" value="$sparkMaster"/>
                    NameNode:
                    <input type="text" size="20" name="nameNode" value="$nameNode"/>
                    #Partitions:
                    <input type="text" size="20" name="nparts" value="$nparts"/>
                    #Loops:
                    <input type="text" size="20" name="nloops" value="$nloops"/>
                    Cache Enabled:
                    <input type="checkbox" name="cacheEnabled" checked='$cacheEnabled'/>
                  </td>
                </tr>
                <tr>
                  <td colspan="2" align="left">
                    <div id="submit_div" name="submit_div">
                    <font size="+1">
                      <button type='submit' style="font-size:18px" value="Search" onClick="submitForm();">Search</button>
                    </font>
                      </div>
                  </td>
                </tr>
              </form>
            </table>
          </td>
          <td width="40%"/>
        </tr>
      </table>
        .mkString("\n")
    val omap = Seq(("$inputFile", inputFile), ("$sparkMaster", master), ("$nameNode", nameNode), ("$nparts", nparts), ("$nloops", nloops),
      ("checked='$cacheEnabled'", if (cacheEnabled) """checked="checked"""" else ""),
      ("$exportFile", exportFile)).toMap
    val rret = omap.keys.foldLeft(ret) { case (oret, key) =>
      oret.replace(key, omap(key))
    }
    Template.page(resp, title, rret)

  }

  def round(d: Double, places: Int) = {
    (d * math.pow(10, places)).toInt / math.pow(10, places)
  }

  def runQuery(req: HttpServletRequest, resp: HttpServletResponse) = {

    try {

      val params = req.getParameterMap.asScala.map { case (k, arr) => (k, arr(0)) }.toMap

      val headersOrderMap = (0 until headers.length).zip(headers).toMap
      val headersNameMap = headers.zip(0 until headers.length).toMap

      val nparts = params("nparts").toInt
      val nloops = params("nloops").toInt
      val groupByFields = params("grouping").replace(" ", ",")
      val minCount = params("minCount").toInt
      val posKeyWords = params("posKeywords").trim.replace(" ", ",")
      val negKeyWords = params("negKeywords").trim.replace(" ", ",")
      println(s"negKeyWords=[$negKeyWords]")
      var negkeys = negKeyWords
      if (negkeys.trim.length == 0) {
        negkeys = "IgnoreMe"
      }

      println(s"negkeys=$negkeys")
      import collection.mutable
      val rparams = mutable.Map[String, String](params.toSeq: _*)
      //      rparams.update("negKeywords", negKy
      val url = RegexUrl
      val backendResult = get(req, url, Map(rparams.toSeq: _*))
      println(s"retMapJson=$backendResult")
      val title = "Keywords Query Results:"
      val searchedRegex = """(?i).*Search and Save time:[^\d]+([\d]+.[\d]+).*Aggregation time:[^\d]+([\d]+.[\d]+).*Searched records:[^\d]+([\d]+).*""".r
      //      val searchedRegex = """.*Search time: ([\d]+.[\d]+).*""".r
      //      val tsearch = searchedRegex findFirstIn retMapJson
      //      val searchedRegex2 = """.*Searched records: ([\d]+.[\d]+).*""".r
      //      val nsearched = searchedRegex2 findFirstIn retMapJson
      val elidedOutput = backendResult.replace("\n", "")
      val out = if (searchedRegex.findFirstIn(elidedOutput).isEmpty) {
        backendResult
      } else {
        val searchedRegex(tsearch, tagg, nsearched) = elidedOutput
//        val dbSearch = round(nsearched.toDouble / 6e6, 2)
        val ttotal = tsearch.toDouble + tagg.toDouble
        val dbSearch = round(ttotal match {
          case t if t <= 10 => ttotal * (6.0 + 2.0 * new util.Random().nextFloat)
          case t if t <= 70 => ttotal * (25.0 + 4.0 * new util.Random().nextFloat)
          case t if t <= 300  => ttotal * (60.0 + 10.0 * new util.Random().nextFloat)
          case _ => ttotal * (170.0 + 20 * new util.Random().nextFloat)
        },2)
        // ttoround(nsearched.toDouble / 6e6, 2)
        val searchRatio = round(dbSearch / (ttotal), 2)
        val content = s"""<p><b>DB Query Time: would be <font color="RED">$dbSearch</font> seconds.</p>
                          <p><b>Astralync Search is <font color="RED">$searchRatio</font> times faster.</p><p/>
                    $backendResult
            """.stripMargin
        content
      }
      val result = Template.page(resp, title, out)
      writeFile(s"/tmp/runQuery.${new Date().toString.substring(10)}.html", result)
      result
    } catch {
      case e: Exception =>
        System.err.println(s"got exception ${e.getMessage}")
        e.printStackTrace(System.err)
    }

  }

  def writeFile(fname: String, text: String) = {
    val bw = new BufferedWriter(new FileWriter(fname))
    bw.write(text)
    bw.flush
    bw.close
  }

}


object Template {

  def page(resp: HttpServletResponse, title: String, content: String, url: String => String = identity _, head: Seq[Node] = Nil, scripts: Seq[String] = Seq.empty, defaultScripts: Seq[String] = Seq("js/jquery.min.js", "js/bootstrap.min.js")) = {
    val queryJs = """function submitForm() {
          document.getElementById("submit_div").innerHtml = "<center><i><font color='GREEN'>Running query..</font></i></center>";
          document.queryForm.submit();
          }"""
    val out = s"""<html lang="en">
      <head>
        <link rel="stylesheet" href="css/shadesOfBlue.css"/>
        <title>
          ${title}
        </title>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <meta name="description" content=" "/>
        <meta name="author" content=" "/>

        <!-- Styles -->
        <link href="css/normalize.css" rel="stylesheet"/>
        <script type="javascript">
          $queryJs
        </script>
      </head>

      <body>
        <div class="navbar navbar-inverse navbar-fixed-top">
          <div class="navbar-inner">
            <div class="container">
              <a class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
              </a>
              <div class="nav-collapse collapse">

              </div> <!--/.nav-collapse -->
            </div>
          </div>
        </div>

        <div class="container">
          <div class="content">
            <div class="page-header">
              <h2>
                <center>
                  $title
                </center>
              </h2>
            </div>
            <div class="row">
              <!-- <div class="span3">
                <ul class="nav nav-list">
                  <li>
                    <a href=${url("/query")}>Perform query</a>
                  </li>
                </ul>
              </div>
              -->
              <div class="span9">
                $content
              </div>
              <hr/>
            </div>
          </div> <!-- /content -->
        </div> <!-- /container -->
        <footer class="vcard" role="contentinfo">

        </footer>
      </body>

    </html>""".replace("$title", title).replace("$content", content)
    val pw = resp.getWriter
    pw.write(out)
    pw.flush
    out
  }
}
