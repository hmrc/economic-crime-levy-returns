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
import uk.gov.hmrc.economiccrimelevyreturns.services.{DataRetrievalService, ReturnValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class ReturnValidationController @Inject() (
  cc: ControllerComponents,
  dataRetrievalService: DataRetrievalService,
  authorise: AuthorisedAction,
  returnValidationService: ReturnValidationService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with BaseController
    with ErrorHandler {

  def getValidationErrors(id: String): Action[AnyContent] = authorise.async { _ =>
    (for {
      eclReturn <- dataRetrievalService.get(id).asResponseError
      _         <- returnValidationService.validateReturn(eclReturn).asResponseError
    } yield ()).convertToResult
  }

}
