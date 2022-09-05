import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "6.4.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % "0.68.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"  % "6.4.0",
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-28" % "0.68.0",
    "org.scalatestplus"   %% "scalacheck-1-15"         % "3.2.10.0",
    "org.scalatestplus"   %% "mockito-3-4"             % "3.2.10.0",
    "com.vladsch.flexmark" % "flexmark-all"            % "0.36.8"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}
