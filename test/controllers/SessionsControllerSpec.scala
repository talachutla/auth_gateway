package controllers

import com.google.inject.{Injector, Key, TypeLiteral}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import controllers.auth.Sessions
import helpers.DefaultSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import com.mohiva.play.silhouette.test._
import generators.UserGenerators._
import models.{User, UserRepo}
import silhouette.DefaultEnv
import scala.concurrent.duration._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{BindingKey, bind}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global


class SessionsControllerSpec extends DefaultSpec with Results with GuiceOneAppPerSuite {

  describe("make") {
    describe("user is not signed it") {
      it("renders loginForm") {
        val controller = app.injector.instanceOf(classOf[Sessions])
        val result = controller.make().apply(FakeRequest())
        status(result) shouldBe 200
        val page = contentAsString(result)
        page should include("email")
        page should include("password")
      }
    }

    describe("user is signed in") {
      it("redirects to root with flash message") {
        val identity: User = userGen().sample.get
        val userRepo = app.injector.instanceOf[UserRepo]
        val future = userRepo.create(identity)
        Await.result(future, 1000 millis)
        val controller = app.injector.instanceOf(classOf[Sessions])
        import net.codingwell.scalaguice.InjectorExtensions._
        // get google injector from play (needed for scalaguice implicits)
        val injector = app.injector.instanceOf[Injector]
        val silhouette = injector.instance[Silhouette[DefaultEnv]]
        implicit val env = silhouette.env
        val request = FakeRequest().withAuthenticator(identity.toLoginInfo)
        val result = controller.make(request)
        status(result) shouldBe 303
      }
    }
  }
}