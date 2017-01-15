package models

import java.sql.Timestamp

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile

import scala.concurrent.Future

abstract class ModelRepo[T](protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  abstract class BasicTable(tag: Tag, name: String) extends Table[T](tag, name) {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def createdAt = column[Timestamp]("created_at", O.AutoInc)
    def updatedAt = column[Timestamp]("updated_at", O.AutoInc)
  }

  type ModelTable <: BasicTable

  def modelsQuery: TableQuery[ModelTable]
  def find(id: Long): Future[Option[T]] = db.run(filteredQuery(id).result.headOption)
  def filteredQuery(id: Long): Query[ModelTable, T, Seq] = for (entity <- modelsQuery; if entity.id === id) yield { entity }


//  abstract class Queries[U <: ModelTable] {
//    def all: Future[Seq[T]] = db.run(modelsQuery.result)
//    def find(id: Long): Future[Option[T]] = db.run(filterQuery(id).result.headOption)
//    def create(entity: T): Future[Long] = db.run(createQuery(entity))
//
//    protected def modelsQuery: TableQuery[U]
//    private val filterQuery = (id: Long) => for (entity <- modelsQuery; if entity.id === id) yield { entity }
//    private val createQuery = (entity: T) => modelsQuery returning modelsQuery.map(_.id) += entity
//  }
}