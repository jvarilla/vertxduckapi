package com.example.starter

import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject

/***
 * Converts a JsonObject to a Duck
 */
fun JsonObject.toDuck(): Duck {
  return Duck(
    this.getString("id"),
    this.getString("name") ?: "no name",
    this.getString("origin") ?: "wandering",
    this.getString("material") ?: "unknown"
  )
}

/***
 * Send a message as a json string with a status code
 */
fun HttpServerResponse.sendAsJSONWithStatusCode(message:String, code:Int) {
  this.putHeader("content-type", "application/json")
    .setStatusCode(code)
    .setChunked(true)
    .write(message)
    .end()
}
