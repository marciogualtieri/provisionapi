package dal.instance

import models.Instance

import scala.concurrent.Future

trait InstanceRepository {
  def create(name: String, plan: String, state: String, targetId: String): Future[Instance]
  def createOrUpdate(instance: Instance): Future[Int]
  def delete(id: Long): Future[Int]
  def get(id: Long): Future[Option[Instance]]
  def getAll: Future[Seq[Instance]]
}
