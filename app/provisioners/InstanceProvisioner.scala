package provisioners

import scala.concurrent.Future

trait InstanceProvisioner {
  def create(name: String, plan: String): Future[(String, String)]
  def delete(targetId: String): Future[Boolean]
  def get(targetId: String): Future[String]
}
