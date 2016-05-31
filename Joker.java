package tasks;

import java.util.concurrent.TimeUnit;

import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import messages.*;
import akka.actor.*;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akkachat.*;

public class Joker extends UntypedActor {
	
	private ActorRef jokeChannel = null;
	private final String identifyId = "1";
	private final Cancellable cancellable;
	private final ExecutionContext ec = context().system().dispatcher();
	
	/* This object is sent to Joker actor */
	private final Object checkJokes = new Object();
	
	/* In the constructor of this actor, the scheduler is implemented 
	 * which sends message to itself after every 50 milliseconds   
	 * */
	public Joker() {
		cancellable = context().system().scheduler().schedule(FiniteDuration.Zero(), 
				FiniteDuration.create(50, TimeUnit.MILLISECONDS), self(), checkJokes, ec, self());
	}
	
	@Override
	public void preStart() {
		context().system().eventStream().subscribe(self(), NewSession.class);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		
		/* This 'if' is executed after every 50ms and 
		 * check about the jokes channel 
		 * */
		if (msg == checkJokes) {
			ActorSelection chk = context().actorSelection("/user/channels/jokes");
			chk.tell(new Identify(identifyId), self());
			
			/* It will executed after ensuring that jokes channel is now present
			 * Also add Joker actor, as a user, in the channel 
			 * Also creates JokeGenerator as a child and cancels further ticks to be sent 
			 *  */
			if (jokeChannel != null){
				jokeChannel.tell(new AddUser(self()), self());
				context().actorOf(Props.create(JokeGenerator.class), "jokegenerator");
				cancellable.cancel();
			}
			
		} else if (msg instanceof ActorIdentity) {
			ActorIdentity identity = (ActorIdentity) msg; 
			if (identity.correlationId().equals(identifyId)) {
				/* This will store the reference of jokes channel */
				jokeChannel = identity.getRef();
			}
			
		} else if (msg instanceof String) {
			/* If jokes channel exists then only it will sends jokes to it */
			if (jokeChannel != null) {
				String joke = (String) msg;
				jokeChannel.tell(new ChatMessage("Joker", joke), self());
			}
			
		} else
			unhandled(msg);
	}
	
	/* This is the strategy to supervise the child 'JokeGenerator' actor */
	@Override
	public SupervisorStrategy supervisorStrategy() {
		Function<Throwable, SupervisorStrategy.Directive> decider = new Function<Throwable, SupervisorStrategy.Directive>() {

			@Override
			public Directive apply(Throwable t) throws Exception {
				if (t instanceof DidNotGetJokeException) {
					return SupervisorStrategy.resume();
				} else if (t instanceof JokeConnectionClosedException) {
					return SupervisorStrategy.restart();
				} else {
					return SupervisorStrategy.escalate();
				}
			}
		};
		return new OneForOneStrategy(10, Duration.create("1 minute"), decider);	
	}
}