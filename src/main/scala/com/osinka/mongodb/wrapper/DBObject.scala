package com.osinka.mongodb.extractors

// Used now in ShapeFields#extract
// dibblego: Another option is a Higher-Order Function, like
// def k(a: Any) = if(a.isInstanceOf[String]) Some(a.asInstanceOf[String])else None
// add a method to Any: 
// def asString[X](f: String => X, n: => X) = if(a.isInstanceOf[String]) f(a.asInstanceOf[String]) else n 
// notice the similarity ala cata

object DBObject { 
  import com.mongodb.{DBObject=>JDBObject}
  // Extractor: unapply works in pattern matches
  def apply(thing: Any): Option[JDBObject] = { 
    if    (thing.isInstanceOf[JDBObject]) 
      Some(thing.asInstanceOf[JDBObject]) 
    else None 
  } 
}
