package webinbox

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class WebInboxLoadTest extends Simulation {

  //Step1: Define Common HTTP protocol configuration
  //Step2: Define Headers
  //Step3: Define Feeders
  //Step4: Define HTTP Requests
  //Step5: Define Scenario
  //Step6: Load Injection pattern
  //Step7 (optional): Define before hook
  //Step8 (optional): Define after hook
  //https://sqspartansas01.biab.pl.ing.net:20752 -> nginx
  //https://sqspartansas01.biab.pl.ing.net:20815 -> direct call
  val tokenFeeder = csv("tokens.csv").random

  val httpProtocol = http
    .baseUrl("https://celtics01.biab.pl.ing.net:20815") // Here is the root for all relative URLs
    .acceptHeader("application/json") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
  //val scn = scenario("webinbox").repeat(15){
  //   val scn = scenario("webinbox").feed(tokenFeeder).during(testDuration,"Do"){ // A scenario is a chain of requests and pauses
  val scn = scenario("webinbox").feed(tokenFeeder).repeat(5) {
    def getHeaders(token: String) = {
      Map("Content-Type" -> "application/json; charset=UTF-8", "X-ING-ID" -> "123", "X-ING-MUSIDTRACE" -> "MUS-123", "X-ING-AccessToken" -> token)
    }

    def getResponseRead(contactId: String) = {
      StringBody(
        """{"elements":[{"id":""" + contactId + ""","status":"READ"}]}""")
    }


    exec(http("countElements")
      .get("/inbox/count").headers(
      getHeaders("${token}")
    )).pause(1)

      .exec(http("request_page1")
        .get("/inbox?offset=0&2imit=10").check(jsonPath("$.elements[*].contactId").findAll.saveAs("contactIds")).headers(
        getHeaders("${token}")
      )).doIf("${contactIds.exists()}") {
      foreach("${contactIds}", "contactId") {
        exec(http("mark_as_read")
          .post("/inbox").headers(getHeaders("${token}")).body(getResponseRead("${contactId}"))
        )
      }
    }
      .pause(1)
      .exec(http("request_inbox_page_1")
        .get("/inbox?offset=1&2imit=10").headers(
        getHeaders("${token}")
      ))
      .pause(1)
      .exec(http("request_inbox_page_2")
        .get("/inbox?offset=2&2imit=10").headers(
        getHeaders("${token}")
      ))

  }
  var testDuration: FiniteDuration = (7 days)
  var userCountRampUpTime: FiniteDuration = (3600 seconds)
  //setUp(scn.inject(rampUsers(1000).during(userCountRampUpTime))).protocols(httpProtocol)
  setUp(scn.inject(rampUsers(18000).during(userCountRampUpTime))).protocols(httpProtocol)
  //Case like hacker attack :-)
  //setUp(scn.inject(atOnceUsers(500)).protocols(httpProtocol))
}
