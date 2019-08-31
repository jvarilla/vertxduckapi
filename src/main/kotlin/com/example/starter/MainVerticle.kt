package com.example.starter


import com.fasterxml.jackson.core.JsonParseException
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.DecodeException
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.*


class MainVerticle : CoroutineVerticle() {
    // Hosting Configuration Settings
    private val API_PORT = 8091
    private var ducksMap: HashMap<Int,Duck> = HashMap()
    private var ducksIdCounter = 0


    //Call your api methods here
    override suspend fun start() {
      // Creating a verticle and server
      val vertx = Vertx.vertx()
      val httpServer = vertx.createHttpServer()
      val router = Router.router(vertx)

      // Add body parsing capability to router
      router.route().handler(BodyHandler.create())


      // API v1 base
      val apiBase1 = "/api/v1"

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
      router.post("$apiBase1/ducks").coroutineHandler(this::createDucksHandler)

      // Put a duck
      router.put("$apiBase1/ducks/:id").coroutineHandler(this::updateDuckHandler)
      // Removed a duck
      router.delete("$apiBase1/ducks/:id").coroutineHandler(this::deleteDuckHandler)

      // Start Server
      httpServer.requestHandler(router)
        .listen(API_PORT)

      println("Verticle Running")

    }


    // Event Handlers

    // Get the duck
    private suspend fun getDuckHandler(routingContext:RoutingContext) :Unit{ // Get one duck
        val request = routingContext.request()
        val response = routingContext.response()
        var responseText = ""
        var statusCode = 200

        try { // try finding duck by id
          // Get the target integer
          val targetId: Int = request.getParam("id").toInt()

          // Find the duck
          val targetDuck = ducksMap[targetId] ?: throw DuckNotFoundException()
          responseText = Json.encodePrettily(targetDuck)
          response.sendAsJSONWithStatusCode(responseText, statusCode)

        } catch (e: Exception) { // if duck not found
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
              responseText = Json.encodePrettily("Invalid Id")
            }
            is JsonParseException -> { // Could not parse the body
              // Change Status Code - Bad Request
              statusCode = 400

              // Set Bad Request Message
              responseText = Json.encodePrettily("Bad Request")
            }
            else -> {
              // Change Status Code - Bad Request
              statusCode = 500

              // Set Bad Request Message
              responseText = Json.encodePrettily("Server Error")
            }
          }
          // Send a message
          val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
          response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
        }

    }

    // Get a list of ducks
    private suspend fun listDucksHandler(routingContext:RoutingContext) {
        val response = routingContext.response()
        var responseText:String
        var statusCode:Int
        try { // Get List of Ducks
          responseText = Json.encodePrettily(ducksMap.values)
          statusCode = 200
          response.sendAsJSONWithStatusCode(responseText, statusCode)
        } catch (e:Exception) { // Catch for any errors
          responseText = "Server Error"
          statusCode = 500
          val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
          response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
        }

    }

    private suspend fun createDucksHandler(routingContext:RoutingContext) { // create a duck
      val response = routingContext.response()

      var responseText = ""
      var statusCode = 200

      try {
        // Parse body for duck info
        val newDuckStr = routingContext.bodyAsString
        // getBodyAsString()
        val newDuckJson = JsonObject(newDuckStr)

        // Create new Duck Object
        val newId = ducksIdCounter

        newDuckJson.put("id", ducksIdCounter.toString())
        // Increment the ducksIdCounter
        ducksIdCounter++

        // Create a new duck
        val newDuck = newDuckJson.toDuck()

        // Add the new duck to the in memory list
        ducksMap[newId] = newDuck

        responseText = Json.encodePrettily(newDuck)

        // Send the response
        response.sendAsJSONWithStatusCode(responseText, statusCode)

      } catch (e:Exception) {
        when (e) {
          is JsonParseException -> { // Unable to parse Json
            // Change Status Code - Bad Request
             statusCode = 400

            // Set Bad Request Message
            responseText = Json.encodePrettily("Bad Request")
          }
          is DecodeException -> { // Invalid Body
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = Json.encodePrettily("Bad Request")
          }
          is IllegalStateException -> { // Missing data
            statusCode = 400

            responseText = Json.encodePrettily("Missing Data")
          }
          else -> {
            // Change Status Code - Server Error
            statusCode = 500
            println(e)
            // Set Bad Request Message
            responseText = Json.encodePrettily("Server Error")
          }
        }
        val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
        response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
      }
    }

    // update a duck
    private suspend fun updateDuckHandler(routingContext:RoutingContext) {
      val request = routingContext.request()
      val response = routingContext.response()

      var responseText = ""
      var statusCode = 200

      try { // try finding duck by id
        // Get the target integer
        val targetId: String = request.getParam("id").toString()

        if (!ducksMap.containsKey(targetId.toInt())) {
          throw DuckNotFoundException()
        }

        val newDuckStr = routingContext.bodyAsString

        // getBodyAsString()
        val newDuckJson = JsonObject(newDuckStr)

        // Create new Duck Object
        newDuckJson.put("id", targetId)

        val newDuck = newDuckJson.toDuck()

        ducksMap[targetId.toInt()] = newDuck

        responseText = Json.encodePrettily(newDuck)


        // Send the response
        response.sendAsJSONWithStatusCode(responseText, statusCode)

      } catch (e: Exception) { // if duck not found
        when (e) {
          is DuckNotFoundException -> { // if duck not found
            // Change Status Code - Bad Request
            statusCode = 404

            // Set Bad Request Message
            responseText = "Duck Not Found"
          }
          is NumberFormatException -> { // Invalid Id
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = Json.encodePrettily("Invalid Id")
          }
          is JsonParseException -> { // Unable to parse Json
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = Json.encodePrettily("Bad Request")
          }
          is DecodeException -> { // Invalid Body
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = Json.encodePrettily("Bad Request")
          }
          is IllegalStateException -> { // Missing data
            statusCode = 400

            responseText = Json.encodePrettily("Missing Data")
          }
          else -> {
            // Change Status Code - Server Error
            statusCode = 500
            // Set Bad Request Message
            responseText = Json.encodePrettily("Server Error")
          }
        }
        val errorMessageObj = Json.encodePrettily(ErrorMessageClass(responseText))
        response.sendAsJSONWithStatusCode(errorMessageObj, statusCode)
      }
    }

    private suspend fun deleteDuckHandler(routingContext: RoutingContext) { // delete a duck
      val request = routingContext.request()
      val response = routingContext.response()

      var responseText = ""
      var statusCode = 204

      try { // try finding duck by id
        // Get the target integer
        val targetId: Int = request.getParam("id").toInt()
        if (!ducksMap.containsKey(targetId.toInt())) {
          throw DuckNotFoundException()
        }

        // Remove the duck
        ducksMap.remove(targetId)

        // Send the response
        response.sendAsJSONWithStatusCode(responseText, statusCode)

      } catch (e: Exception) { // if duck not found
        when (e) {
          is DuckNotFoundException -> { // if duck not found
            // Change Status Code - Bad Request
            statusCode = 404

            // Set Bad Request Message
            responseText = "Duck Not Found"
          }
          is NumberFormatException -> { // Invalid Id
            // Change Status Code - Bad Request
            statusCode = 400

            // Set Bad Request Message
            responseText = Json.encodePrettily("Invalid Id")
          }
          else -> {
            // Change Status Code - Server Error
            statusCode = 500

            // Set Bad Request Message
            responseText = Json.encodePrettily("Server Error")
          }
        }
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

    // Send message with status code
    private fun HttpServerResponse.sendAsJSONWithStatusCode(message:String, code:Int) {
      this.putHeader("content-type", "application/json")
        .setStatusCode(code)
        .setChunked(true)
        .write(message)
        .end()
    }

}

