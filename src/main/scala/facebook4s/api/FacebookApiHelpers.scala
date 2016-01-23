package facebook4s.api

trait FacebookApiHelpers {
  /** Converts a sequence of key/value pairs into a map of key to sequence of values if and
   *  only if the value is defined (not None).
   *  Used to create a Map[K, Seq[V]].
   *  @param keyValue pairs to include in map
   *  @return the map
   */
  protected def buildModifiers(keyValue: (String, Option[Any])*): Map[String, Seq[String]] = keyValue
    // keep defined values
    .filter(_._2.isDefined)
    // group by key, creating: k1 -> Seq(k1->v1, k2->v2, ...)
    .groupBy(kv ⇒ kv._1)
    // keep key grouping, change values from k1->v1 to v1
    .mapValues(group ⇒ group.map(x ⇒ x._2.get.toString))

  protected def buildRelativeUrl(parts: Any*): String = parts
    .filter(p ⇒ p != None && p != null)
    .map {
      case Some(a) ⇒ a.toString
      case a       ⇒ a
    }
    .mkString("/")
}

