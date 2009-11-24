package com.osinka.mongodb.shape

import com.mongodb.DBCollection
import com.osinka.mongodb.{MongoObject,QueriedCollection}

trait Implicits {
    implicit def collOfShape(coll: DBCollection) = new {
        def of[T](element: ObjectShape[T]) = new ShapedCollection[T](coll, element)
    }

    implicit def collWithQuery[T](q: Queriable[T]#ShapeQuery) = new {
        def in[Coll <: QueriedCollection[T, Coll]](coll: QueriedCollection[T, Coll]): Coll = coll.applied(q.query)
    }

    implicit def collWithQuery[T <: MongoObject](qt: QueryTerm[T]) = new {
        def in(coll: ShapedCollection[T]): ShapedCollection[T] = {
            val shapeQuery = coll.shape where qt
            coll.applied(shapeQuery.query)
        }
    }
}