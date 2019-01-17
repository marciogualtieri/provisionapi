package provisioners

import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2ClientBuilder}
import com.amazonaws.services.ec2.model._
import javax.inject.Inject
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

class AmazonInstanceProvisioner @Inject()(config: Configuration)(implicit ec: ExecutionContext) extends InstanceProvisioner {

  val client: AmazonEC2 = AmazonEC2ClientBuilder.defaultClient

  override def create(name: String, plan: String): Future[(String, String)] = Future {
    val nameTagSpec = buildTagSpecificationForName(name)
    val request = buildRunInstanceRequest(plan, nameTagSpec)
    val result = client.runInstances(request)
    val targetId = result.getReservation.getInstances.get(0).getInstanceId
    val state = result.getReservation.getInstances.get(0).getState.getName
    (targetId, state)
  }

  override def delete(targetId: String): Future[Boolean] = Future {
    val terminateInstancesRequest = new TerminateInstancesRequest
    terminateInstancesRequest.withInstanceIds(targetId)
    val result = client.terminateInstances(terminateInstancesRequest)
    result.getTerminatingInstances.size > 0
  }

  override def get(targetId: String): Future[String] = Future {
    val describeInstanceRequest = buildDescribeInstanceRequest(targetId)
    val result = client.describeInstanceStatus(describeInstanceRequest)
    if (result.getInstanceStatuses.isEmpty) "unavailable"
    else result.getInstanceStatuses.get(0).getInstanceState.getName
  }

  private def buildTagSpecificationForName(name: String) = {
    val nameTag = new Tag("Name", name)
    val tagSpecification = new TagSpecification()
    tagSpecification.withTags(nameTag)
    tagSpecification.withResourceType(ResourceType.Instance)
    tagSpecification
  }

  private def buildRunInstanceRequest(plan: String, tagSpecification: TagSpecification) = {
    val request = new RunInstancesRequest
    request.withImageId(config.get[String]("amazon.image.id"))
      .withInstanceType(InstanceType.valueOf(plan))
      .withMinCount(1)
      .withMaxCount(1)
      .withKeyName(config.get[String]("amazon.key.pair.name"))
      .withSecurityGroups(config.get[String]("amazon.security.group.name"))
      .withTagSpecifications(tagSpecification)
    request
  }

  private def buildDescribeInstanceRequest(targetId: String) = {
    val request = new DescribeInstanceStatusRequest()
    request.setIncludeAllInstances(true)
    request.withInstanceIds(targetId)
    request
  }

}
