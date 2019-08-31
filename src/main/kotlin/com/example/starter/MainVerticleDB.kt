package com.example.starter


import com.fasterxml.jackson.core.JsonParseException
import io.netty.util.Timer
import io.vertx.core.Handler
import java.util.UUID
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.mongo.impl.MongoClientImpl
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*


class MainVerticleDB : CoroutineVerticle() {

  // Hosting Configuration Settings
  private val DB_HOST_ADDRESS = "127.0.0.1"
  private val DB_PORT = 27017
  private val API_PORT = 8092


  private val dbConfiguration = Json.encodePrettily(DBConfig(DB_HOST_ADDRESS, DB_PORT))
  private val vertx1 = Vertx.vertx()

  // Database Configuration Settings
  private val configObj = JsonObject(dbConfiguration)

  // Instantiate an interface for mocking
  private var objectRepository :ObjectRepository = ObjectRepositoryImpl(vertx1, configObj)

    //Call your api methods here
    override suspend fun start() {

      // Creating a verticle and server
      val httpServer = vertx1.createHttpServer()
      val router = Router.router(vertx1)

      // Add body parsing capability to router
      router.route().handler(BodyHandler.create())


      // API v1 base
      val apiBase1 = "/api/v2"

      // Default Route
      router.get("$apiBase1/")
        .handler { routingContext ->
          val response = routingContext.response()
          response.putHeader("content-type", "text/plain")
            .setChunked(true)
            .write("Welcome to the Ducks API").end()
        }

      // Get all ducks
      router.get("$apiBase1/ducks").coroutineHandler(this::listDucksHandler)

      // Get a duck by id
      router.get("$apiBase1/ducks/:id").coroutineHandler(this::getDuckHandler)

      // Post a duck
      router.post("$apiBase1/ducks").coroutineHandler(this::createDuckHandler)

      // Put a duck
      router.put("$apiBase1/ducks/:id").coroutineHandler(this::updateDuckHandler)

      // Removed a duck
      router.delete("$apiBase1/ducks/:id").coroutineHandler(this::deleteDuckHandler)

      // Start Server
      httpServer.requestHandler(router)
        .listen(API_PORT)

      println("Verticle DB Running")

    }


    // Event Handlers

    // Get One Duck
    private suspend fun getDuckHandler(routingContext:RoutingContext) {

      val request = routingContext.request()
      val response = routingContext.response()
      var responseText = ""
      var statusCode = 200

      try { // try finding duck by id
        // Get the target integer
        val targetId: String = request.getParam("id").toString()

        // Get the duck record

        val duckRecord = objectRepository.readDuckRecord(targetId)

        responseText = Json.encodePrettily(duckRecord)

        // Send The Duck
        response.sendAsJSONWithStatusCode(responseText, statusCode)

      } catch (e: Exception) {
        when (e) {
          is DuckNotFoundException -> { // if duck not found
            // Change Status Code - Bad Request
            statusCode = 404

            // Set Bad Request Message
            responseText = "Duck Not Found"
          }
          is NumberFormatException -> {
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = "Invalid Id"
          }
          is JsonParseException -> { // Could not parse the body
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = "Bad Request"
          }
          else -> {
            // Change Status Code - Bad Request
            statusCode = 500

            // Set Bad Request Message
            responseText = "Server Error"
          }
        }

        // Send a message
        val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
        response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
      }
    }

    // List ducks
    private suspend fun listDucksHandler(routingContext:RoutingContext) { // get list of ducks
      val response = routingContext.response()
      var responseText = ""

      var statusCode:Int
      try { // Get List of Ducks
        // Get list of all ducks in collection
        val ducksList = objectRepository.listAllDuckRecords()
        responseText = Json.encodePrettily(ducksList)
        statusCode = 200
        // Send the Response
        response.sendAsJSONWithStatusCode(responseText, statusCode)
      } catch (e:Exception) { // Catch for any errors
        responseText = "Server Error"
        statusCode = 500
        // Send the Response
        response.sendAsJSONWithStatusCode(responseText, statusCode)
      }
    }

    // Create a new duck
    private suspend fun createDuckHandler(routingContext:RoutingContext) { // create a duck
        val response = routingContext.response()

        var responseText = ""
        var statusCode = 200

        try {

          // Parse body for duck info
          val newDuckStr = routingContext.bodyAsString

          // Make a new Duck from the body
          val newDuckJson = JsonObject(newDuckStr)
          newDuckJson.put("id", UUID.randomUUID().toString())
          val newDuck = newDuckJson.toDuck()

          // Save the duck to the database
          val savedDuck = objectRepository.createDuckRecord(newDuck)

          // Send Back a Response
          responseText = Json.encodePrettily(savedDuck)
          response.sendAsJSONWithStatusCode(responseText, statusCode)

        } catch (e:Exception) {
          when (e) {
            is JsonParseException -> { // Unable to parse Json
              // Change Status Code - Bad Request
              statusCode = 400

              // Set Bad Request Message
              responseText = "Bad Request"
            }
            is DecodeException -> { // Invalid Body
              // Change Status Code - Bad Request
              statusCode = 400

              // Set Bad Request Message
              responseText = "Bad Request"
            }
            is IllegalStateException -> { // Missing data
              statusCode = 400

              responseText = "Missing Data"
            }
            else -> {
              // Change Status Code - Server Error
              statusCode = 500
              println(e)
              // Set Bad Request Message
              responseText = "Server Error"
            }
          }
          // Send Back a Response
          val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
          response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
        }
    }

    // Update an existing duck
    private suspend fun updateDuckHandler(routingContext:RoutingContext) { // update a duck
      val request = routingContext.request()
      val response = routingContext.response()

      var responseText = ""
      var statusCode = 200

      try { // try finding duck by id
        // Get the target integer
        val targetId: String = request.getParam("id").toString()

        // Parse body for duck info
        val newDuckStr = routingContext.bodyAsString
        val newDuckObj = JsonObject(newDuckStr).put("id", targetId)
        val newDuck = newDuckObj.toDuck()

        // Update the database
        val updatedDuck = objectRepository.updateDuckRecord(targetId, newDuck)
        responseText = Json.encodePrettily(updatedDuck)
        // Send the Response
        response.sendAsJSONWithStatusCode(responseText, statusCode)
      } catch (e: Exception) { // if duck not found
        when (e) {
          is DuckNotFoundException -> {
            statusCode = 404
            responseText = "404 Duck Not Found"
          }
          is NumberFormatException -> { // Invalid Id
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = "Invalid Id"
          }
          is JsonParseException -> { // Unable to parse Json
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = "Bad Request"
          }
          is DecodeException -> { // Invalid Body
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = "Bad Request"
          }
          is IllegalStateException -> { // Missing data
            statusCode = 400

            responseText = "Missing Data"
          }
          else -> {
            // Change Status Code - Server Error
            statusCode = 500
            // Set Bad Request Message
            responseText = "Server Error"
          }
        }
        // Send the Response
        val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
        response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
      }
    }

    // Delete a duck
    private suspend fun deleteDuckHandler(routingContext: RoutingContext) { // delete a duck
      val request = routingContext.request()
      val response = routingContext.response()

      var responseText = ""
      var statusCode = 204

      try { // try finding duck by id
        // Get the target integer
        val targetId: String = request.getParam("id").toString()

        // Delete the duck from the database
        objectRepository.deleteDuckRecord(targetId)

        response.sendAsJSONWithStatusCode(responseText, statusCode)

      } catch (e: Exception) { // if duck not found
        when (e) {
          is DuckNotFoundException -> {
            statusCode = 404
            responseText = "404 Duck Not Found"
          }
          is NumberFormatException -> { // Invalid Id
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = "Invalid Id"
          }
          else -> {
            // Change Status Code - Server Error
            statusCode = 500

            // Set Bad Request Message
            responseText = "Server Error"
          }
        }
        // Send the Response
        val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
        response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
      }
    }

    // This is needed to make coroutines work. Don't worry about how it works for now.
    private fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
      handler { ctx ->
        launch(ctx.vertx().dispatcher()) {
          try {
            fn(ctx)
          } catch (e: Throwable) {
            ctx.fail(e)
          }
        }
      }
    }

  }


