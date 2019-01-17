package controllers

import java.text.SimpleDateFormat

import actions.AuthenticatedUserAction
import javax.inject._
import dal.instance.InstanceSlickRepository
import models._
import play.api.Configuration
import play.api.mvc._
import play.api.libs.json.{JsValue, Json, Writes}
import provisioners.AmazonInstanceProvisioner

import scala.concurrent.ExecutionContext

class ProvisionApiController @Inject()(instanceRepository: InstanceSlickRepository, cc: MessagesControllerComponents,
                                       authenticatedUserAction: AuthenticatedUserAction,
                                       instanceProvisioner: AmazonInstanceProvisioner, config: Configuration)
                                      (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val dateFormat = new SimpleDateFormat(config.get[String]("json.date.format"))

  val instanceWrites: Writes[Instance] = (instance: Instance) => Json.obj(
    "id" -> instance.id,
    "name" -> instance.name,
    "plan" -> instance.plan,
    "state" -> instance.state,
    "targetId" -> instance.targetId,
    "created" -> dateFormat.format(instance.created),
    "updated" -> dateFormat.format(instance.updated)
  )

  val simplifiedInstanceWrites: Writes[Instance] = (instance: Instance) => Json.obj(
    "id" -> instance.id,
    "name" -> instance.name,
    "plan" -> instance.plan,
  )

  def index: Action[AnyContent] = Action {
    Ok(views.html.index())
  }

  def getAllInstances: Action[AnyContent] = authenticatedUserAction.async {
    implicit val writes: Writes[Instance] = simplifiedInstanceWrites
    instanceRepository.getAll.map { instance =>
      Ok(Json.toJson(instance))
    }
  }

  def provisionInstance(): Action[JsValue] = authenticatedUserAction.async(parse.json) {
    implicit val writes: Writes[Instance] = instanceWrites
    implicit request => {
      val (name: String, plan: String) = parseProvisionParameters(request).getOrElse {
        BadRequest("INVALID JSON.")
      }

      for {
        (targetId, state) <- instanceProvisioner.create(name, plan)
        instance <- instanceRepository.create(name, plan, state, targetId)
      } yield Created(Json.toJson(instance))
    }
  }

  def getInstance(id: Long): Action[AnyContent] = authenticatedUserAction.async {
    implicit val writes: Writes[Instance] = instanceWrites
    (for {
      Some(instance) <- instanceRepository.get(id)
      state <- instanceProvisioner.get(instance.targetId)
    } yield Ok(Json.toJson(instance.withState(state))))
      .recover {
        case _: NoSuchElementException => handleInstanceIdDoesNotExist(id)
    }
  }

  def deprovisionInstance(id: Long): Action[AnyContent] = authenticatedUserAction.async {
    (for {
      Some(instance) <- instanceRepository.get(id)
      _ = instanceProvisioner.delete(instance.targetId)
      _ = instanceRepository.createOrUpdate(instance)
    } yield Ok(s"ID=[$id] DELETED."))
      .recover {
        case _: NoSuchElementException => handleInstanceIdDoesNotExist(id)
      }
  }

  private def parseProvisionParameters(request: Request[JsValue]) = {
    {
      for {name <- (request.body \ "name").asOpt[String]
           plan <- (request.body \ "plan").asOpt[String]
      } yield (name, plan)
    }
  }

  private def handleInstanceIdDoesNotExist(id: Long) = {
    NotFound(s"ID=[$id] DOES NOT EXIST.")
  }

}