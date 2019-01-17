package helpers

import java.text.SimpleDateFormat

import actions.AuthenticatedUserAction
import controllers.ProvisionApiController
import dal.instance.InstanceSlickRepository
import models.Instance
import org.mockito.Mockito._
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeHeaders
import utils.TimeUtils
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.db.DBApi
import play.api.test.Helpers.contentAsString
import provisioners.AmazonInstanceProvisioner
import play.api.libs.functional.syntax._
import java.sql.Timestamp
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

trait TestHelper extends MockitoSugar {

  val testNowTimestamp: Timestamp = new Timestamp(System.currentTimeMillis())

  val mockTimeUtils: TimeUtils = mock[TimeUtils]
  when(mockTimeUtils.now()).thenReturn(testNowTimestamp)

  val databaseConfigProvider: DatabaseConfigProvider = InjectorHelper.inject[DatabaseConfigProvider]
  implicit val executionContext: ExecutionContext = InjectorHelper.inject[ExecutionContext]
  val testInstanceSlickRepository = new InstanceSlickRepository(mockTimeUtils, databaseConfigProvider)
  val testMessagesControllerComponents: MessagesControllerComponents = InjectorHelper.inject[MessagesControllerComponents]
  val testAuthenticatedUserAction: AuthenticatedUserAction = InjectorHelper.inject[AuthenticatedUserAction]

  val testInstance = Instance(id = 1, name = "Test Instance", plan = "T2Micro",
    state = "disabled", targetId ="amazon123", created = testNowTimestamp, updated = testNowTimestamp)

  val anotherTestInstance = Instance(id = 2, name = "Another Test Instance", plan = "T2Micro",
    state = "disabled", targetId = "amazon123", created = testNowTimestamp, updated = testNowTimestamp)

  val simplifiedTestInstance: (Long, String, String) = (testInstance.id, testInstance.name, testInstance.plan)

  val anotherSimplifiedTestInstance: (Long, String, String) = (anotherTestInstance.id, anotherTestInstance.name,
    anotherTestInstance.plan)

  val testInstanceRequestJson: JsValue = Json.parse(
    s"""
      |{
      |  "name": "${testInstance.name}",
      |  "plan": "${testInstance.plan}"
      |}
    """.stripMargin)

  val testToken = "bWFyY2lvZ3VhbHRpZXJpOmRkamtsbXJydmN2Y3VpbzQzNA=="

  val invalidTestToken = "NOT_A_VALID_TOKEN"

  val testPostHeaders = FakeHeaders(Seq("Content-type" -> "application/json", "X-AUTH" -> testToken))

  val missingAuthenticationTestPostHeaders = FakeHeaders(Seq("Content-type" -> "application/json"))

  val invalidTestPostHeaders = FakeHeaders(Seq("Content-type" -> "application/json", "X-AUTH" -> invalidTestToken))

  val testHeaders = FakeHeaders(Seq("X-AUTH" -> testToken))

  val invalidTokenTestHeaders = FakeHeaders(Seq("X-AUTH" -> invalidTestToken))

  lazy val testDatabaseApi: DBApi = InjectorHelper.inject[DBApi]

  val testConfig: Configuration = InjectorHelper.inject[Configuration]

  val mockAmazonProvisioner: AmazonInstanceProvisioner = mock[AmazonInstanceProvisioner]

  val testProvisionApiController = new ProvisionApiController(testInstanceSlickRepository,
    testMessagesControllerComponents, testAuthenticatedUserAction, mockAmazonProvisioner, testConfig)


  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")

  implicit object timestampFormat extends Format[Timestamp] {
    def reads(json: JsValue): JsSuccess[Timestamp] = {
      val str = json.as[String]
      JsSuccess(new Timestamp(dateFormat.parse(str).getTime))
    }
    def writes(ts: Timestamp) = JsString(dateFormat.format(ts))
  }

  implicit val instanceWrites: Writes[Instance] = (instance: Instance) => Json.obj(
    "id" -> instance.id,
    "name" -> instance.name,
    "plan" -> instance.plan,
    "state" -> instance.state,
    "created" -> dateFormat.format(instance.created),
    "updated" -> dateFormat.format(instance.updated)
  )

  def instanceFromResult(result: Future[Result]): Instance = {
    implicit val instanceReads: Reads[Instance] = (
      (JsPath \ "id").read[Long] and
        (JsPath \ "name").read[String] and
        (JsPath \ "plan").read[String] and
        (JsPath \ "state").read[String] and
        (JsPath \ "targetId").read[String] and
        (JsPath \ "created").read[Timestamp] and
        (JsPath \ "updated").read[Timestamp]
      )(Instance.apply _)
    Json.fromJson[Instance](Json.parse(contentAsString(result))).get
  }

  def instancesFromResult(result: Future[Result]): Seq[(Long, String, String)] = {
    implicit val instanceReads: Reads[(Long, String, String)] = (
      (JsPath \ "id").read[Long] and
        (JsPath \ "name").read[String] and
        (JsPath \ "plan").read[String]
      )(Tuple3.apply[Long, String, String] _)
    Json.fromJson[Seq[(Long, String, String)]](Json.parse(contentAsString(result))).get
  }

  def setupMockAmazonProvisioner(): Unit = {
    reset(mockAmazonProvisioner)

    when(mockAmazonProvisioner.create(testInstance.name, testInstance.plan))
      .thenReturn(Future {
        Tuple2(testInstance.targetId, testInstance.state)
      })

    when(mockAmazonProvisioner.create(anotherTestInstance.name, anotherTestInstance.plan))
      .thenReturn(Future {
        Tuple2(anotherTestInstance.targetId, anotherTestInstance.state)
      })

    when(mockAmazonProvisioner.get(testInstance.targetId))
      .thenReturn(Future {
        testInstance.state
      })

    when(mockAmazonProvisioner.get(anotherTestInstance.targetId))
      .thenReturn(Future {
        anotherTestInstance.state
      })
  }

  val testExceptionMessage = "Some Exception"
  val testProvisioningErrorMessage = s"ERROR: ${testExceptionMessage}."

}
