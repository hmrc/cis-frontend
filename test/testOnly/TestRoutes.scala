package testOnly

import controllers.monthlyreturns.SubcontractorDetailsAddedController
import models.{CheckMode, NormalMode}
import play.api.routing.{Router, SimpleRouter}
import play.api.routing.sird.*
import javax.inject.Inject

class TestRoutes @Inject() (controller: SubcontractorDetailsAddedController) extends SimpleRouter {

  override def routes: Router.Routes = {
    // >>> CHANGE <<< only include routes needed by this spec
    case GET(p"/monthly-return/subcontractor-details-added") =>
      controller.onPageLoad(NormalMode)

    case POST(p"/monthly-return/subcontractor-details-added") =>
      controller.onSubmit(NormalMode)

    case GET(p"/changeSubcontractorDetailsAdded") =>
      controller.onPageLoad(CheckMode)

    case POST(p"/changeSubcontractorDetailsAdded") =>
      controller.onSubmit(CheckMode)
  }
}
