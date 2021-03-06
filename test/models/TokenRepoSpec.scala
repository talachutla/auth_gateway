package models

import helpers.{AsyncPropertyChecks, DatabaseCleaner, DefaultPropertyChecks, DefaultSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TokenRepoSpec extends DefaultSpec with DefaultPropertyChecks with DatabaseCleaner with GuiceOneAppPerSuite  {
  val tokenRepo = app.injector.instanceOf(classOf[TokenRepo])
  import generators.TokenGenerators._
  import generators.BasicGenerators._

  // Use this to insert duplicate users for tests (User repo API prohibits this)
  def createTokenWithSql(token: Token): Unit = {
    database.withConnection { connection =>
      val statement = connection.createStatement()
      val sql = s"""INSERT INTO tokens (value, action, payload, expiration_time)
                   |VALUES ('${token.value}', '${token.action}', '${token.payload}', '${token.expirationTime}');
                 """.stripMargin
      statement.execute(sql)
    }
  }

  describe("find") {
    describe("token with specified value is in the db and unique") {
      it("returns Some(token)") {
        forAllAsync { token: Token =>
          for {
            _ <- tokenRepo.create(token)
            foundToken <- tokenRepo.find(token.value)
          } yield {
            foundToken.isEmpty shouldBe false
            foundToken.get.value shouldBe token.value
          }
        }
      }
    }

    describe("no tokens with specified value exist") {
      it("returns None") {
        forAllAsync { token: Token =>
          for {
            foundToken <- tokenRepo.find(token.value)
          } yield {
            foundToken.isEmpty shouldBe true
          }
        }
      }
    }

    describe("two tokens with the same value are in the db") {
      it("throws AlreadyExists error") {
        forAll { token: Token =>
          createTokenWithSql(token)
          createTokenWithSql(token)
          val future = tokenRepo.find(token.value)
          ScalaFutures.whenReady(future.failed) { e =>
            e shouldBe a[ModelsExceptions.TooManyFoundException[_]]
          }
        }
      }
    }
  }

  describe("create") {
    describe("no tokens with specified value exist") {
      it("creates a new token") {
        forAllAsync { token: Token =>
          for {
            _ <- tokenRepo.create(token)
            foundToken <- tokenRepo.find(token.value)
          } yield {
            foundToken.isEmpty shouldBe false
            foundToken.get.value shouldBe token.value
          }
        }
      }
    }

    describe("token with specified value already exists") {
      it("throws an AlreadyExists exception") {
        forAll {token: Token =>
          createTokenWithSql(token)
          val future = tokenRepo.create(token)
          ScalaFutures.whenReady(future.failed) { e =>
            e shouldBe a[ModelsExceptions.AlreadyExists[_]]
          }
        }
      }
    }
  }

  describe("delete") {
    it("deletes all entries in the db and returns count") {
      forAllAsync(smallNumGen, tokenGen()) { (n, token) =>
        for (i <- 1 to n) yield createTokenWithSql(token)
        for {
          result <- tokenRepo.delete(token.value)
        } yield {
          result shouldBe n
        }
      }
    }
  }
}
