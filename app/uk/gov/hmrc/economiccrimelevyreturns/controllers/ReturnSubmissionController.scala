/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyreturns.controllers

import cats.data.EitherT
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.config.AppConfig
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyreturns.models.integrationframework.{EclReturnSubmission, SubmitEclReturnResponse}
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, EclReturn, FirstTimeReturn}
import uk.gov.hmrc.economiccrimelevyreturns.services._
import uk.gov.hmrc.economiccrimelevyreturns.utils.CorrelationIdHelper
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnSubmissionController @Inject() (
  cc: ControllerComponents,
  authorise: AuthorisedAction,
  returnValidationService: ReturnValidationService,
  integrationFrameworkService: IntegrationFrameworkService,
  nrsService: NrsService,
  dmsService: DmsService,
  auditService: AuditService,
  appConfig: AppConfig,
  returnsService: ReturnsService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController
    with ErrorHandler {

  def getSubmission(periodKey: String, id: String): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.headerCarrierWithCorrelationId(request)
    (for {
      eclReturn <- integrationFrameworkService.getEclReturnSubmission(periodKey, id).asResponseError
    } yield eclReturn).convertToResult(OK)
  }

  def submitReturn(id: String): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.headerCarrierWithCorrelationId(request)
    (for {
      eclReturn           <- returnsService.get(id).asResponseError
      eclReturnSubmission <- returnValidationService.validateReturn(eclReturn).asResponseError
      eclReturnResponse   <- routeReturnSubmission(eclReturn, eclReturnSubmission)
    } yield eclReturnResponse).convertToResult(OK)
  }

  private def routeReturnSubmission(eclReturn: EclReturn, eclReturnSubmission: EclReturnSubmission)(implicit
    request: AuthorisedRequest[_],
    hc: HeaderCarrier
  ): EitherT[Future, ResponseError, SubmitEclReturnResponse] =
    eclReturn.returnType match {
      case Some(FirstTimeReturn) =>
        firstTimeSubmission(
          request.eclRegistrationReference,
          eclReturnSubmission,
          eclReturn,
          appConfig.firstTimeReturnNotableEvent
        )
      case Some(AmendReturn)     =>
        amendSubmission(eclReturn, request.eclRegistrationReference)
      case None                  =>
        EitherT.left[SubmitEclReturnResponse](
          Future.successful(ResponseError.internalServiceError(cause = None))
        )
    }

  private def firstTimeSubmission(
    eclReference: String,
    eclReturnSubmission: EclReturnSubmission,
    eclReturn: EclReturn,
    eventName: String
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]) =
    for {
      base64EncodedNrsSubmissionHtml <- checkOptionalValueExists[String](eclReturn.base64EncodedNrsSubmissionHtml)
      submitEclReturnResponse        <-
        integrationFrameworkService.submitEclReturn(eclReference, eclReturnSubmission).asResponseError
      _                              <- nrsService.submitToNrs(base64EncodedNrsSubmissionHtml, eclReference, eventName).asResponseError
      _                               = auditService.sendReturnSubmittedEvent(eclReturn, eclReference, submitEclReturnResponse.chargeReference)

    } yield submitEclReturnResponse

  private def amendSubmission(eclReturn: EclReturn, eclRegistrationReference: String)(implicit
    hc: HeaderCarrier,
    request: AuthorisedRequest[_]
  ): EitherT[Future, ResponseError, SubmitEclReturnResponse] =
    for {
      base64EncodedDmsSubmissionHtml <- checkOptionalValueExists[String](eclReturn.base64EncodedDmsSubmissionHtml)
      base64EncodedNrsSubmissionHtml <- checkOptionalValueExists[String](eclReturn.base64EncodedNrsSubmissionHtml)
      _                              <- if (appConfig.amendReturnsNrsEnabled) {
                                          nrsService
                                            .submitToNrs(
                                              base64EncodedNrsSubmissionHtml,
                                              eclRegistrationReference,
                                              appConfig.amendReturnNotableEvent
                                            )
                                            .asResponseError
                                        } else {
                                          EitherT.right(Future.successful(()))
                                        }

      eclReturnResponse <- dmsService.submitToDms(base64EncodedDmsSubmissionHtml, Instant.now).asResponseError
      _                  = auditService.sendReturnSubmittedEvent(eclReturn, eclRegistrationReference, None)
    } yield eclReturnResponse

}
