import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "9.8.0"
  private val hmrcMongoVersion     = "2.5.0"
  private val openHtmlToPdfVersion = "1.0.10"


  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % hmrcMongoVersion,
    "com.openhtmltopdf"  % "openhtmltopdf-pdfbox"         % openHtmlToPdfVersion,
    "org.typelevel"     %% "cats-core"                    % "2.10.0",
    "io.circe"          %% "circe-json-schema"            % "0.2.0",
    "org.json"          %  "json"                         % "20240303",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "3.0.0",
    "io.circe"          %% "circe-parser"                 % "0.14.6",
    "com.beachape"      %% "enumeratum-play-json"         % "1.8.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"   % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-test-play-30"  % hmrcMongoVersion,
    "org.mockito"          %% "mockito-scala"            % "1.17.30",
    "org.scalatestplus"    %% "scalacheck-1-17"          % "3.2.18.0",
    "com.danielasfregola"  %% "random-data-generator"    % "2.9",
    "io.circe"             %% "circe-json-schema"        % "0.2.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"    % "1.1.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}