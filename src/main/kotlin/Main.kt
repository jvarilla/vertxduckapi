
import io.vertx.core.Vertx

fun main(args: Array<String>) {
  println("hello")
  val vertx = Vertx.vertx()
  val vertx2 = Vertx.vertx()
  // Deploy a verticle
  vertx.deployVerticle("com.example.starter.MainVerticleDB")
  vertx2.deployVerticle("com.example.starter.MainVerticle")
}
