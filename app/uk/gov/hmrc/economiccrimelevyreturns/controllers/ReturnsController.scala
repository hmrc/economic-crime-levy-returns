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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.economiccrimelevyreturns.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyreturns.models.{AdditionalInfo, EclReturn}
import uk.gov.hmrc.economiccrimelevyreturns.repositories.{InfoRepository, ReturnsRepository}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class ReturnsController @Inject() (
  cc: ControllerComponents,
  returnsRepository: ReturnsRepository,
  infoRepository: InfoRepository,
  authorise: AuthorisedAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def upsertReturn: Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[EclReturn] { eclReturn =>
      returnsRepository.upsert(eclReturn).map(_ => Ok(Json.toJson(eclReturn)))
    }
  }

  def getReturn(id: String): Action[AnyContent] = authorise.async { _ =>
    returnsRepository.get(id).map {
      case Some(eclReturn) => Ok(Json.toJson(eclReturn))
      case None            => NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "Return not found")))
    }
  }

  def deleteReturn(id: String): Action[AnyContent] = authorise.async { _ =>
    returnsRepository.clear(id).map(_ => NoContent)
  }

  def upsertInfo: Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[AdditionalInfo] { info =>
      infoRepository.upsert(info).map(_ => Ok(Json.toJson(info)))
    }
  }

  def getInfo(id: String): Action[AnyContent] = authorise.async { _ =>
    infoRepository.get(id).map {
      case Some(info) => Ok(Json.toJson(info))
      case None       => NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "No additional info")))
    }
  }

  def deleteInfo(id: String): Action[AnyContent] = authorise.async { _ =>
    infoRepository.clear(id).map(_ => NoContent)
  }
}
