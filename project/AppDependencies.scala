import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "7.19.0"
  private val hmrcMongoVersion     = "1.3.0"
  private val openHtmlToPdfVersion = "1.0.10"


  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % hmrcMongoVersion,
    "com.openhtmltopdf"  % "openhtmltopdf-pdfbox"      % openHtmlToPdfVersion,
    "org.typelevel"     %% "cats-core"                 % "2.9.0",
    "io.circe"          %% "circe-json-schema"         % "0.2.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-28"      % "1.6.0",
    "io.circe"          %% "circe-parser"              % "0.14.5",
    "uk.gov.hmrc"       %% "internal-auth-client-play-28" % "1.6.0",
    "com.beachape"      %% "enumeratum-play-json"     % "1.7.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-28"   % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-test-play-28"  % hmrcMongoVersion,
    "org.mockito"          %% "mockito-scala"            % "1.17.12",
    "org.scalatestplus"    %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "com.danielasfregola"  %% "random-data-generator"    % "2.9",
    "io.circe"             %% "circe-json-schema"        % "0.2.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"    % "1.1.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}