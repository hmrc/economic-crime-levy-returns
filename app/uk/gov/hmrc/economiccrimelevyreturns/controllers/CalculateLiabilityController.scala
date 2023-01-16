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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.CalculateLiabilityRequest
import uk.gov.hmrc.economiccrimelevyreturns.services.CalculateLiabilityService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton()
class CalculateLiabilityController @Inject() (
  cc: ControllerComponents,
  authorise: AuthorisedAction,
  calculateLiabilityService: CalculateLiabilityService
) extends BackendController(cc) {

  def calculateLiability: Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[CalculateLiabilityRequest] { calculateLiabilityRequest =>
      Future.successful(Ok(Json.toJson(calculateLiabilityService.calculateLiability(calculateLiabilityRequest))))
    }
  }

}
