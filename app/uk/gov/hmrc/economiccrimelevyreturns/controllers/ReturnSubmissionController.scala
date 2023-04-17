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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyreturns.repositories.ReturnsRepository
import uk.gov.hmrc.economiccrimelevyreturns.services.{ReturnService, ReturnValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnSubmissionController @Inject() (
  cc: ControllerComponents,
  returnsRepository: ReturnsRepository,
  authorise: AuthorisedAction,
  returnValidationService: ReturnValidationService,
  returnService: ReturnService
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def submitReturn(id: String): Action[AnyContent] = authorise.async { implicit request =>
    returnsRepository.get(id).flatMap {
      case Some(eclReturn) =>
        returnValidationService.validateReturn(eclReturn) match {
          case Valid(eclReturnDetails) =>
            returnService.submitEclReturn(request.eclRegistrationReference, eclReturnDetails, eclReturn).map {
              response =>
                Ok(Json.toJson(response))
            }
          case Invalid(e)              =>
            Future.successful(InternalServerError(Json.toJson(DataValidationErrors(e.toList))))
        }
      case None            => Future.successful(NotFound)
    }
  }

}
