package com.osinka.mongodb.shape

import scala.reflect.Manifest
import com.mongodb.DBObject
import com.osinka.mongodb.Preamble.tryo
import com.osinka.mongodb.wrapper.DBO
import com.osinka.mongodb.{MongoObject,Serializer}

/*
 * Basic object/field shape
 */
trait BaseShape[Type, Rep] {
    def extract(x: Rep): Option[Type] // a kind of "unapply"
    def pack(v: Type): Rep // a kind of "apply"

    /**
     * Constraints on collection object to have this "shape"
     */
    def constraints: Map[String, Map[String, Boolean]]
}

/*
 * Shape of an object backed by DBObject ("hosted in")
 */
trait ObjectShape[T]
        extends BaseShape[T, DBObject]
        with Serializer[T]
        with ShapeFields[T, T]
        with Queriable[T] {

    def * : List[Field[T, _]]
    def factory(dbo: DBObject): Option[T]

    // -- BaseShape[T,R]
    override lazy val constraints = (* remove {_.mongo_?} foldLeft Map[String,Map[String,Boolean]]() ) { (m,f) =>
        assert(f != null, "Field must not be null")
        m ++ f.constraints
    }

    override def extract(dbo: DBObject) = factory(dbo) map { x =>
        assert(x != null, "Factory should not return Some(null)")
        for {val f <- * if f.isInstanceOf[HostUpdate[_,_]]
             val fieldDbo <- tryo(dbo.get(f.fieldName))}
            f.asInstanceOf[HostUpdate[T,_]].updateUntyped(x, fieldDbo)
        x
    }

    override def pack(x: T): DBObject =
        DBO.fromMap(
            (* foldLeft Map[String,Any]() ) { (m,f) =>
                assert(f != null, "Field must not be null")
                m + (f.fieldName -> f.valueOf(x))
            }
        )

    // -- Serializer[T]
    override def in(obj: T) = pack(obj)

    override def out(dbo: DBObject) = extract(dbo)

    override def mirror(x: T)(dbo: DBObject) = {
        for {val f <- * if f.mongo_? && f.isInstanceOf[HostUpdate[_,_]]
             val fieldDbo <- tryo(dbo.get(f.fieldName))}
            f.asInstanceOf[HostUpdate[T,_]].updateUntyped(x, fieldDbo)
        x
    }
}

/**
 * Mix-in to make a shape functional, see FunctionalTransformer for explanation
 *
 * FunctionalShape make a shape with convinient syntactic sugar
 * for converting object to DBObject (apply) and extractor for the opposite
 *
 * E.g.
 * val dbo = UserShape(u)
 * dbo match {
 *    case UserShape(u) =>
 * }
 */
trait FunctionalShape[T] { self: ObjectShape[T] =>
    def apply(x: T): DBObject = pack(x)
    def unapply(rep: DBObject): Option[T] = extract(rep)
}

/**
 * Shape of MongoObject child.
 *
 * It has mandatory _id and _ns fields
 */
trait MongoObjectShape[T <: MongoObject] extends ObjectShape[T] {
    import com.mongodb.ObjectId

    object oid extends Scalar[ObjectId]("_id", _.mongoOID)
            with Functional[ObjectId]
            with Mongo[ObjectId]
            with Updatable[ObjectId] {
        override def update(x: T, oid: ObjectId): Unit = x.mongoOID = oid
    }
    object ns extends Scalar[String]("_ns", _.mongoNS)
            with Functional[String]
            with Mongo[String]
            with Updatable[String] {
        override def update(x: T, ns: String): Unit = x.mongoNS = ns
    }

    // -- ObjectShape[T]
    override def * : List[Field[T, _]] = oid :: ns :: Nil
}