package com.example.starter

import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

data class Duck(var id:String, var name:String, var origin:String, var material:String) {
  fun toJsonObject() :JsonObject {
    return jsonObjectOf(
      "id" to this.id,
      "name" to this.name,
      "origin" to this.origin,
      "material" to this.material
    )
  }
}
