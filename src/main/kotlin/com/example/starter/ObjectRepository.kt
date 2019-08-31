package com.example.starter

import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext

interface ObjectRepository {
  suspend fun createDuckRecord(duck:Duck) :Duck

  suspend fun readDuckRecord(id:String) :Duck

  suspend fun updateDuckRecord(id:String, newDuck:Duck) :Duck

  suspend fun deleteDuckRecord(id:String) :Boolean

  suspend fun listAllDuckRecords() : List<Duck>

}
