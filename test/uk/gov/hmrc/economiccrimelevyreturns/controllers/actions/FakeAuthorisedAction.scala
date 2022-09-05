/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyreturns.controllers.actions

import play.api.mvc._
import uk.gov.hmrc.economiccrimelevyreturns.models.requests.AuthorisedRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeAuthorisedAction @Inject() (bodyParsers: PlayBodyParsers) extends AuthorisedAction {

  override def parser: BodyParser[AnyContent] = bodyParsers.defaultBodyParser

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
    block(AuthorisedRequest(request, "id"))

  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

}
