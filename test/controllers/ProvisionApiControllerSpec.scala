package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.Await
import helpers.TestHelper
import org.scalatest.BeforeAndAfter
import play.api.db.evolutions.Evolutions

import scala.concurrent.duration.Duration

class ProvisionApiControllerSpec extends PlaySpec with GuiceOneAppPerTest
   with BeforeAndAfter with TestHelper {


  before {
    Evolutions.applyEvolutions(testDatabaseApi.database("default"))
    setupMockAmazonProvisioner()
  }

  after {
    Evolutions.cleanupEvolutions(testDatabaseApi.database("default"))
  }

  "ProvisionApiController GET" should {

    "render the index page" in {
      val request = FakeRequest(GET, "/")
      val home = route(app, request).get

      status(home) mustBe OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("SERVICE IS LIVE!")
    }

    "render an instance" in {
      Await.result(testInstanceSlickRepository.create(testInstance.name, testInstance.plan, testInstance.state,
        testInstance.targetId), Duration.Inf)
      val result = testProvisionApiController.getInstance(testInstance.id).apply(
        FakeRequest(GET, controllers.routes.ProvisionApiController.getInstance(testInstance.id).url, testHeaders,
          null, null)
      )

      status(result) mustBe OK
      instanceFromResult(result) mustBe testInstance

    }

    "respond with <not found> when instance does not exist" in {
      val result = testProvisionApiController.getInstance(testInstance.id).apply(
        FakeRequest(GET, controllers.routes.ProvisionApiController.getInstance(testInstance.id).url, testHeaders,
          null, null)
      )

      status(result) mustBe NOT_FOUND
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) must include(s"ID=[${testInstance.id}] DOES NOT EXIST.")
    }

    "deny access to render an instance without an authentication token" in {
      val result = testProvisionApiController.getAllInstances().apply(
        FakeRequest(GET, controllers.routes.ProvisionApiController.getInstance(testInstance.id).url)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"MISSING AUTHENTICATION TOKEN.")
    }

    "deny access to render instance with an invalid authentication token" in {
      val result = testProvisionApiController.getAllInstances().apply(
        FakeRequest(GET, controllers.routes.ProvisionApiController.getInstance(testInstance.id).url,
          invalidTokenTestHeaders, null, null)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"INVALID AUTHENTICATION TOKEN.")
    }

    "render all instances" in {
      Await.result(testInstanceSlickRepository.create(testInstance.name, testInstance.plan, testInstance.state,
        testInstance.targetId),
        Duration.Inf)
      Await.result(testInstanceSlickRepository.create(anotherTestInstance.name, anotherTestInstance.plan,
        anotherTestInstance.state, testInstance.targetId), Duration.Inf)

      val result = testProvisionApiController.getAllInstances().apply(
        FakeRequest(GET, controllers.routes.ProvisionApiController.getAllInstances().url, testHeaders, null, null)
      )

      status(result) mustBe OK
      instancesFromResult(result) mustBe Seq(simplifiedTestInstance, anotherSimplifiedTestInstance)

    }

    "deny access to render all instances without an authentication token" in {
      val result = testProvisionApiController.getAllInstances().apply(
        FakeRequest(GET, controllers.routes.ProvisionApiController.getAllInstances().url)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"MISSING AUTHENTICATION TOKEN.")
    }

    "deny access to render all instances with an invalid authentication token" in {
      val result = testProvisionApiController.getAllInstances().apply(
        FakeRequest(GET, controllers.routes.ProvisionApiController.getAllInstances().url,
          invalidTokenTestHeaders, null, null)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"INVALID AUTHENTICATION TOKEN.")
    }
  }

  "ProvisionApiController POST" should {

    "provision a new instance" in {
      val result = testProvisionApiController.provisionInstance().apply(
        FakeRequest(POST,
          controllers.routes.ProvisionApiController.provisionInstance().url,
          testPostHeaders, testInstanceRequestJson)
      )

      status(result) mustBe CREATED
      instanceFromResult(result) mustBe testInstance
    }

    "deny access to provisioning an instance without authentication token" in {
      val result = testProvisionApiController.provisionInstance().apply(
        FakeRequest(POST, controllers.routes.ProvisionApiController.provisionInstance().url,
          missingAuthenticationTestPostHeaders, testInstanceRequestJson)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"MISSING AUTHENTICATION TOKEN.")
    }

    "deny access to provisioning an instance with an invalid authentication token" in {
      val result = testProvisionApiController.provisionInstance().apply(
        FakeRequest(POST, controllers.routes.ProvisionApiController.provisionInstance().url,
          invalidTestPostHeaders, testInstanceRequestJson)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"INVALID AUTHENTICATION TOKEN.")
    }

  }

  "ProvisionApiController DELETE" should {

    "de-provision an instance" in {
      Await.result(testInstanceSlickRepository.create(testInstance.name, testInstance.plan,
        testInstance.state, testInstance.targetId), Duration.Inf)
      val result = testProvisionApiController.deprovisionInstance(testInstance.id).apply(
        FakeRequest(DELETE,
          controllers.routes.ProvisionApiController.deprovisionInstance(testInstance.id).url, testHeaders, null, null)
      )

      status(result) mustBe OK
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) must include(s"ID=[${testInstance.id}] DELETED.")
    }

    "respond with <not found> when an instance does not exist" in {
      val result = testProvisionApiController.deprovisionInstance(testInstance.id).apply(
        FakeRequest(DELETE,
          controllers.routes.ProvisionApiController.deprovisionInstance(testInstance.id).url, testHeaders, null, null)
      )

      status(result) mustBe NOT_FOUND
      contentType(result) mustBe Some("text/plain")
      contentAsString(result) must include(s"ID=[${testInstance.id}] DOES NOT EXIST.")
    }

    "deny access to de-provisioning an instance without authentication token" in {
      val result = testProvisionApiController.deprovisionInstance(testInstance.id).apply(
        FakeRequest(DELETE,
          controllers.routes.ProvisionApiController.deprovisionInstance(testInstance.id).url)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"MISSING AUTHENTICATION TOKEN.")
    }

    "deny access to de-provisioning an instance with an invalid authentication token" in {
      val result = testProvisionApiController.deprovisionInstance(testInstance.id).apply(
        FakeRequest(DELETE,
          controllers.routes.ProvisionApiController.deprovisionInstance(testInstance.id).url,
          invalidTokenTestHeaders, null, null)
      )

      status(result) mustBe FORBIDDEN
      contentAsString(result) must include(s"INVALID AUTHENTICATION TOKEN.")
    }

  }

}
