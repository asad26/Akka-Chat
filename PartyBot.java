package tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import scala.concurrent.ExecutionContext;
import scala.concurrent.duration.FiniteDuration;
import messages.*;
import akka.actor.*;

public class PartyBot extends UntypedActor {
	
	private final ExecutionContext ec = context().system().dispatcher();
	private final String identifyId = "1";
	
	/* This variable stores information about which channel PartyBot has joined */
	private final List<String> channelsJoined = new ArrayList<String>();
	
	/* This variable is used to compare message type */
	private final Object checkSystem = new Object();
	
	/* In the constructor of this actor, the scheduler is implemented 
	 * which sends message to itself after every 5 seconds   
	 * */
	public PartyBot() {
		context().system().scheduler().schedule(FiniteDuration.Zero(), FiniteDuration.create(5, TimeUnit.SECONDS),
				self(), checkSystem, ec, self());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception { 
		
		/* Actor will check the system for new channels */
		if (msg == checkSystem) {
			ActorSelection checkChannel = context().actorSelection("/user/channels/*");
			checkChannel.tell(new Identify(identifyId), self());
			
		} else if (msg instanceof UserAdded) {
			UserAdded uAdded = (UserAdded) msg;
			
			/* This means that PartyBot has joined that channel
			 * by also sending a chat message to that channel
			 *  */
			channelsJoined.add(uAdded.channelName);
			uAdded.channel.tell(new ChatMessage("PartyBot", "Party! Party!"), self());
			
		} else if (msg instanceof UserRemoved) {
			UserRemoved uRemoved = (UserRemoved) msg;
			channelsJoined.remove(uRemoved.channelName);
			
		} else if (msg instanceof ActorIdentity) {
			ActorIdentity identity = (ActorIdentity) msg; 
			if (identity.correlationId().equals(identifyId)) {
				ActorRef ref = identity.getRef();
				
				/* The first condition in if statement shows that there is a new channel in the system,
				 * and second condition shows that the PartyBot actor has not joined it yet
				 * It will join that channel by sending an instance of AddUser message to channel
				 *  */
				if (ref != null && !(channelsJoined.contains(ref.path().name())) ) {
					ref.tell(new AddUser(self()), self());
				}
			}
			
		} else
			unhandled(msg);
	}
}