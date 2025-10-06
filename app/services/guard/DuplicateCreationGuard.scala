package services.guard

import models.requests.DataRequest
import pages.monthlyreturns.MonthlyReturnEntityPage

import javax.inject.Singleton
import scala.concurrent.Future
import play.api.Logging
import services.guard.DuplicateCreationCheck.{DuplicateFound, NoDuplicate}

sealed trait DuplicateCreationCheck
object DuplicateCreationCheck {
  case object NoDuplicate extends DuplicateCreationCheck
  case object DuplicateFound extends DuplicateCreationCheck
}

trait DuplicateCreationGuard {
  def check(implicit request: DataRequest[_]): Future[DuplicateCreationCheck]
}

@Singleton
class DuplicateCreationGuardImpl extends DuplicateCreationGuard with Logging{
  def check(implicit request: DataRequest[_]):Future[DuplicateCreationCheck] =
    val duplicate = request.userAnswers.get(MonthlyReturnEntityPage).isDefined
    Future.successful(if (duplicate) DuplicateFound else NoDuplicate)
}