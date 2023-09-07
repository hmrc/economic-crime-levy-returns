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

import cats.data.Validated.{Invalid, Valid}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}

import java.time.Instant
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.audit.RequestStatus.Success
import uk.gov.hmrc.economiccrimelevyreturns.models.{AmendReturn, FirstTimeReturn}
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.repositories.ReturnsRepository
import uk.gov.hmrc.economiccrimelevyreturns.services.{DmsService, NrsService, ReturnService, ReturnValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnSubmissionController @Inject() (
  cc: ControllerComponents,
  returnsRepository: ReturnsRepository,
  authorise: AuthorisedAction,
  returnValidationService: ReturnValidationService,
  returnService: ReturnService,
  nrsService: NrsService,
  dmsService: DmsService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val loggerContext = "ReturnSubmissionController"

  def submitReturn(id: String): Action[AnyContent] = authorise.async { implicit request =>
    returnsRepository.get(id).flatMap {
      case Some(eclReturn) =>
        logger.info(s"$loggerContext - Found return for id: $id")
        returnValidationService.validateReturn(eclReturn) match {
          case Valid(eclReturnDetails) =>
            logger.info(s"$loggerContext - Successfully validated eclReturnDetails")
            eclReturn.returnType match {
              case Some(FirstTimeReturn) =>
                returnService.submitEclReturn(request.eclRegistrationReference, eclReturnDetails, eclReturn).map {
                  response =>
                    nrsService.submitToNrs(
                      eclReturn.base64EncodedNrsSubmissionHtml,
                      request.eclRegistrationReference
                    )
                    Ok(Json.toJson(response))
                }
              case Some(AmendReturn)     =>
                dmsService.submitToDms(eclReturn.base64EncodedDmsSubmissionHtml, Instant.now).map {
                  case Right(response) =>
                    {
                      logger.info(s"$loggerContext - Successfully submitted PDF to DMS queue")
                      returnService.sendReturnSubmittedEvent(eclReturn, request.eclRegistrationReference, None, Success)
                    }
                    Ok(Json.toJson(response))
                  case Left(error)     =>
                    logger.error(s"$loggerContext - Submission to DMS failed with error ${error.message}")

                    InternalServerError("Could not send PDF to DMS queue")
                }
              case None                  =>
                logger.error(s"$loggerContext - Return type is missing")
                Future.successful(InternalServerError("Return type is missing"))
            }
          case Invalid(e)              =>
            logger
              .error(s"$loggerContext - Validation for EclReturnDetails failed with errors: ${e.toList.mkString(";")}")
            Future.successful(InternalServerError(Json.toJson(DataValidationErrors(e.toList))))
        }
      case None            =>
        logger.error(s"$loggerContext - Return not found for id: $id")
        Future.successful(NotFound)
    }
  }

}
