package com.osinka.mongodb

import com.mongodb.{DBObject, BasicDBObject}
import Helper._

case class Query(val query: DBObject, val skip: Option[Int], val limit: Option[Int]) {
    def slice_? = skip.isDefined || limit.isDefined

    def drop(n: Option[Int]) = Query(query, n map { _+(skip getOrElse 0) } orElse skip, limit)

    def take(n: Option[Int]) = Query(query, skip, n)

    def drop(n: Int): Query = drop(Some(n))

    def take(n: Int): Query = take(Some(n))

    def *(q: Query): Query = ++(q.query) drop q.skip take q.limit

    def ++(q: DBObject): Query = {
        val dbo = emptyDBO
        dbo putAll query
        dbo putAll q
        Query(dbo, skip, limit)
    }

    def in(coll: DBObjectCollection): DBObjectCollection = coll.applied(this)

    def apply(coll: DBObjectCollection) = in(coll)
}

case object EmptyQuery extends Query(Query.Empty, None, None)

object Query {
    val Empty = new BasicDBObject

    def apply(): Query = apply(Empty)
    def apply(q: DBObject) = new Query(q, None, None)

//    case class Term[+T] {
//        def in(r: Range): Term[T]
//
//        def in(seq: Seq[T]): Term[T]
//    }
}

trait QueriedCollection[T] extends MongoCollection[T] {
    type Self <: QueriedCollection[T]

    def query: Query

    def applied(q: Query): Self
    
    override def find = find(query)
    override def firstOption = findOne(query)
    override def sizeEstimate = getCount(query)
}


/*
CollObject.where{_.fieldName < 2}

Query(CollObject).where{c => c.field < 2).drop(10).take(20).findAllIn(coll)
Query(CollObject).findFirstIn(coll)
*/