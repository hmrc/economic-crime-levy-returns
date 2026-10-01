package uk.gov.hmrc.economiccrimelevyreturns.models.hip

import play.api.libs.json.{Reads, __}

final case class HipSuccessWrapper[A](success: A)

object HipSuccessWrapper {
  implicit def reads[A: Reads]: Reads[HipSuccessWrapper[A]] =
    (__ \ "success").read[A].map(HipSuccessWrapper(_))
}
