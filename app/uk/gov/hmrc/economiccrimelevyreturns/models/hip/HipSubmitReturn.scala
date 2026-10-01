package uk.gov.hmrc.economiccrimelevyreturns.models.hip

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.SubmitEclReturnResponse

import java.time.Instant

final case class HipSubmitReturnSuccessResponse(success: HipSubmitReturn)

object HipSubmitReturnSuccessResponse{
  implicit val format: OFormat[HipSubmitReturnSuccessResponse] = Json.format[HipSubmitReturnSuccessResponse]
}

final case class HipSubmitReturn(
                                          processingDate: Instant,
                                          eclReference: String,
                                          submissionId: Option[String],
                                          chargeReference: Option[String],
                                          amount: BigDecimal,
                                          dueDate: String
                                        ) {

  /*
  The IFS response only contains the below fields & is also the response of this endpoints call. Furthermore, the
  requirements of this migration doesn't include a change in the API's response so it's safe to drop
  the extra fields above. Play ignores unused fields so SubmitEclReturnResponse could have been used in the HipConnector
  for serialization but this method makes it clearer the fields are being acknowledged & dropped intentionally.
   */
  def toIfsResponse: SubmitEclReturnResponse =
    SubmitEclReturnResponse(
      processingDate = processingDate,
      chargeReference = chargeReference
    )
}

object HipSubmitReturn {
  implicit val format: OFormat[HipSubmitReturn] = Json.format[HipSubmitReturn]
}
