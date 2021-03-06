package lila.db

import reactivemongo.api._
import reactivemongo.api.commands.GetLastError
import reactivemongo.bson._

trait CollExt { self: dsl with QueryBuilderExt =>

  final implicit class ExtendColl(coll: Coll) {

    def uno[D: BSONDocumentReader](selector: Bdoc): Fu[Option[D]] =
      coll.find(selector).uno[D]

    def list[D: BSONDocumentReader](selector: Bdoc): Fu[List[D]] =
      coll.find(selector).list[D]()

    def list[D: BSONDocumentReader](selector: Bdoc, max: Int): Fu[List[D]] =
      coll.find(selector).list[D](max)

    def byId[D: BSONDocumentReader, I: BSONValueWriter](id: I): Fu[Option[D]] =
      uno[D]($id(id))

    def byId[D: BSONDocumentReader](id: String): Fu[Option[D]] = uno[D]($id(id))

    def byId[D: BSONDocumentReader](id: Int): Fu[Option[D]] = uno[D]($id(id))

    def byIds[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I]): Fu[List[D]] =
      list[D]($inIds(ids))

    def byIds[D: BSONDocumentReader](ids: Iterable[String]): Fu[List[D]] =
      byIds[D, String](ids)

    def countSel(selector: Bdoc): Fu[Int] = coll count selector.some

    def exists(selector: Bdoc): Fu[Boolean] = countSel(selector).dmap(0!=)

    def byOrderedIds[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I], readPreference: ReadPreference = ReadPreference.primary)(docId: D => I): Fu[List[D]] =
      coll.find($inIds(ids)).cursor[D](readPreference = readPreference).
        collect[List](Int.MaxValue, err = Cursor.FailOnError[List[D]]()).
        map { docs =>
          val docsMap = docs.map(u => docId(u) -> u).toMap
          ids.flatMap(docsMap.get).toList
        }

    // def byOrderedIds[A <: Identified[String]: TubeInColl](ids: Iterable[String]): Fu[List[A]] =
    //   byOrderedIds[String, A](ids)

    def optionsByOrderedIds[D: BSONDocumentReader, I: BSONValueWriter](ids: Iterable[I])(docId: D => I): Fu[List[Option[D]]] =
      byIds[D, I](ids) map { docs =>
        val docsMap = docs.map(u => docId(u) -> u).toMap
        ids.map(docsMap.get).toList
      }

    def primitive[V: BSONValueReader](selector: Bdoc, field: String): Fu[List[V]] =
      coll.find(selector, $doc(field -> true))
        .list[Bdoc]()
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitive[V: BSONValueReader](selector: Bdoc, sort: Bdoc, field: String): Fu[List[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .list[Bdoc]()
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitive[V: BSONValueReader](selector: Bdoc, sort: Bdoc, nb: Int, field: String): Fu[List[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .list[Bdoc](nb)
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: Bdoc, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .uno[Bdoc]
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def primitiveOne[V: BSONValueReader](selector: Bdoc, sort: Bdoc, field: String): Fu[Option[V]] =
      coll.find(selector, $doc(field -> true))
        .sort(sort)
        .uno[Bdoc]
        .dmap {
          _ flatMap { _.getAs[V](field) }
        }

    def updateField[V: BSONValueWriter](selector: Bdoc, field: String, value: V) =
      coll.update(selector, $set(field -> value))

    def updateFieldUnchecked[V: BSONValueWriter](selector: Bdoc, field: String, value: V): Unit =
      coll.update(selector, $set(field -> value), writeConcern = GetLastError.Unacknowledged)

    def incField(selector: Bdoc, field: String, value: Int = 1) =
      coll.update(selector, $inc(field -> value))

    def incFieldUnchecked(selector: Bdoc, field: String, value: Int = 1): Unit =
      coll.update(selector, $inc(field -> value), writeConcern = GetLastError.Unacknowledged)

    def unsetField(selector: Bdoc, field: String, multi: Boolean = false) =
      coll.update(selector, $unset(field), multi = multi)

    def fetchUpdate[D: BSONDocumentHandler](selector: Bdoc)(update: D => Bdoc): Funit =
      uno[D](selector) flatMap {
        _ ?? { doc =>
          coll.update(selector, update(doc)).void
        }
      }
  }
}
