import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import akka.actor.ActorRef
import akka.actor.ActorSystem 

case class ProcessStringMsg(string: String)
case class StringProcessedMsg(words: Integer)
case class StartProcessFileMsg()


class WordCounterActor(filename:String) extends Actor{
	private var running = false
	private var totalLines  = 0 
	private var linesProcessed = 0 
	private var result = 0 
	private var fileSender: Option[ActorRef] = None
		def receive = {
		case StartProcessFileMsg() => {
			if(running){
				println("WARNING : Duplicate start message ")
			}else {
				running = true 
				fileSender = Some(sender)
				import scala.io.Source._
				fromFile(filename).getLines.foreach { line => 
					context.actorOf(Props[StringCounterActor]) ! ProcessStringMsg(line)
					totalLines += 1
				}
			}
		}
		case StringProcessedMsg(words) => {
			result += words 
			linesProcessed += 1
			if(linesProcessed == totalLines) {
				fileSender.map(_ ! result)
			}
		}
			case _ => println("Message not recognized ")
	}
}

class StringCounterActor extends Actor{
	def receive = {
		case ProcessStringMsg(string) => {
			val wordsInLine = string.split(" ").length
			sender ! StringProcessedMsg(wordsInLine)
		}
		case _ => {
			println ("Not recognized ")
		}
	}
}
object Sample extends App {
	import akka.util.Timeout
	import scala.concurrent.duration._
	import akka.pattern.ask
	import akka.dispatch.ExecutionContexts._

	implicit val ec = global
	val system = ActorSystem("System")
	val actor = system.actorOf(Props(new WordCounterActor("hello.scala")))
	implicit val timeout = Timeout(25 seconds)
	val future = actor ? StartProcessFileMsg()
	future.map { result => {
			println("Total Number of words " + result)
			system.shutdown
		}
	}
}

