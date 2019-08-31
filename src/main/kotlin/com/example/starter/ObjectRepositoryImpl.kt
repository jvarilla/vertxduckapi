package com.example.starter

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.ext.mongo.*
import java.lang.Exception
import com.example.starter.DuckNotFoundException as DuckNotFoundException

class ObjectRepositoryImpl(val vertx:Vertx, val config:JsonObject): ObjectRepository {
  private val mongoClient = MongoClient.createShared(vertx, config)
  private val response = ""


  // Create a new Duck Record
  override suspend fun createDuckRecord(duck: Duck): Duck {
    val newDuckJson = duck.toJsonObject().put("_id", duck.id)
    newDuckJson.remove("id")

    try {
      mongoClient.saveAwait("ducks", newDuckJson)
      return duck
    } catch (err :Exception) {
      throw err
    }
  }

  // Get One Duck
  override suspend fun readDuckRecord(id: String) :Duck{
    // Make query
    val query = JsonObject().put("_id", id)

    // The empty json object (3rd parameter) gets all fields
    try {
      val res = mongoClient.findOneAwait("ducks", query, jsonObjectOf()) ?: throw DuckNotFoundException()
      res.put("id", res.getString("_id"))
      res.remove("_id")
      return res.toDuck()
    } catch (e :Exception) {
      throw e
    }
  }


  override suspend fun updateDuckRecord(id: String, newDuck: Duck): Duck {
    // Make query
    val query = JsonObject().put("_id", id)
    val updatedDuck = newDuck.toJsonObject()
    updatedDuck.put("_id", id)
    updatedDuck.remove("id")
    try {
      val res = mongoClient.findOneAndReplaceAwait("ducks", query, updatedDuck) ?: throw DuckNotFoundException()
      res.put("id", res.getString("_id"))
      res.remove("_id")
      return newDuck
    } catch (e :Exception) {
      throw e
    }
  }

    // Delete a duck
    override suspend fun deleteDuckRecord(id: String): Boolean {
      // Make the query
      val query = JsonObject().put("_id", id)
      val foundAndDeletedDuck: Boolean
      try { // Delete the document
       // mongoClient.removeDocumentAwait("ducks", query)
        val res = mongoClient.findOneAndDeleteAwait("ducks", query) ?: throw DuckNotFoundException()
        // If the result was null it means the duck doesn't exist
        foundAndDeletedDuck = res != null
      } catch (e :Exception) {
        throw e
      }
      return foundAndDeletedDuck
    }

    // List all ducks
    override suspend fun listAllDuckRecords(): List<Duck> {
      // Get All the ducks in the collection
      val results = mongoClient.findAwait("ducks", JsonObject())

      // Convert the list to an list of ducks
      return  results.map {
        it.put("id", it.getString("_id"))
        it.remove("_id")
        it.toDuck()
      }
    }

  }



