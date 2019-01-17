package dal.instance

import java.sql.Timestamp

import javax.inject.{Inject, Singleton}
import models._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import utils.TimeUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InstanceSlickRepository @Inject()(timeUtils: TimeUtils, dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends InstanceRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val rowToInstance: ((Long, String, String, String, String, Timestamp, Timestamp)) => Instance = {
    case (id, name, plan, state, targetId, created, updated) => Instance(id, name, plan, state, targetId, created, updated)
  }

  val instanceToRow: Instance => Option[(Long, String, String, String, String, Timestamp, Timestamp)] =
    i => Some((i.id, i.name, i.plan, i.state, i.targetId, i.created, i.updated))

  private class InstanceTable(tag: Tag) extends Table[Instance](tag, "instance") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def plan = column[String]("plan")

    def state = column[String]("state")

    def targetId = column[String]("targetId")

    def created: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("created")

    def updated: Rep[java.sql.Timestamp] = column[java.sql.Timestamp]("updated")

    def * = (id, name, plan, state, targetId, created, updated) <> (rowToInstance, instanceToRow)
  }

  private val instances = TableQuery[InstanceTable]

  override def create(name: String, plan: String, state: String, targetId: String): Future[Instance] = db.run {
    (instances.map(i => (i.name, i.plan, i.state, i.targetId, i.created, i.updated))
      returning instances.map(_.id)
      into ((cols, id) => Instance(id, cols._1, cols._2, cols._3, cols._4, cols._5, cols._6))
      ) += (name, plan, state, targetId, timeUtils.now(), timeUtils.now())
  }

  override def createOrUpdate(instance: Instance): Future[Int] = db.run {
    instances.insertOrUpdate(instance.withUpdated(timeUtils.now()))
  }

  override def delete(id: Long): Future[Int] = db.run {
    instances.filter(_.id === id).delete
  }

  override def get(id: Long): Future[Option[Instance]] = db.run {
    instances.filter(_.id === id).result.headOption
  }

  override def getAll: Future[Seq[Instance]] = db.run {
    instances.result
  }

}