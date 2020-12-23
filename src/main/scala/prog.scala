import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import de.heikoseeberger.akkahttpjackson.JacksonSupport

object prog extends JacksonSupport {

// Akka HTTP needs an actor system to run
implicit val system = ActorSystem("Server")

/** *
 * this is an example of akka http GET:
 * remember that HTTP POST supplies additional data from the client (browser) to the server in the message body.
 * In contrast, GET requests include all required data in the URL
 */
// call it like this from a browser: http://127.0.0.1:8080/data/kitteh
// or use: curl -X GET http://127.0.0.1:8080/data/kitteh
// this route just receives a response
val routeGetSimple: Route = get {
  pathPrefix("data") {
    complete("get received")
  }
}

// call it like this: http://127.0.0.1:8080/data/kitteh
// curl -X GET http://127.0.0.1:8080/data/kitteh
// this route just processes the input sent in on the url
val routeGet: Route = get {
  pathPrefix("data") {
    (get & path(Segment)) { data =>
      complete(data.toUpperCase())
    }
  }
}

// have to do posts from curl to put the payload in the http body
// curl -X POST http://127.0.0.1:8080/data/user
// this route just returns a response
val routePutSimple: Route = (path("data" / "user") & post) {
  complete("Post received!")
}

// this route sends a json body which is converted to a case class 'AppRequest'
// returns AppResponse case class
// curl -XPOST  -H "Content-Type: application/json" -d "{\"request\":\"stuff\", \"data\":100}" http://127.0.0.1:8080/api/user
val routePutComplex: Route = (path("api" / "user") & post) {
  entity(as[AppRequest]) { request =>
    complete(AppResponse(s"${request.request}", System.currentTimeMillis()))
  }
}
  // this is a CUSTOM exception handler, which overrides the default handler (recommended)
  // remember NOT to catch general exceptions like Throwable: means that the calling code is reacting to recoverable
  // and irrecoverable situations in the same way, which is not desirable.
  // curl -X POST http://127.0.0.1:8080/data/user
  def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: ArithmeticException =>
        extractUri { uri => complete(HttpResponse(InternalServerError, entity = "Arithmetic App Exception caught"))}
    }
  // associated route which uses the handler
  val routePutException: Route = handleExceptions(myExceptionHandler) {
    (path("data" / "user") & post) {
      1/0 // force error
      // normal routing logic here
      complete()
    }
  }

  def main(args: Array[String]): Unit = {
  // start the server
  // Http().bindAndHandle(route, "127.0.0.1", 8080)  // deprecated interface

    // GET EXAMPLES
  // Http().newServerAt("127.0.0.1", 8080).bind(routeGetSimple)
  //Http().newServerAt("127.0.0.1", 8080).bind(routeGet)

    // PUT EXAMPLES
  // Http().newServerAt("127.0.0.1", 8080).bind(routePutSimple)
  // Http().newServerAt("127.0.0.1", 8080).bind(routePutComplex)

    // ERROR EXAMPLES
    Http().newServerAt("127.0.0.1", 8080).bind(routePutException)

}

case class AppRequest(request: String, data: Int)

case class AppResponse(data: String, timestamp: Long)

}