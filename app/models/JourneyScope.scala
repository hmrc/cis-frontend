package models

import models.requests.CisPath

import java.util.UUID

object JourneyScope {
  def subJourneyId(cisPath: CisPath, uuid: UUID): String =
    s"${cisPath.toUrl}_$uuid"}
