package tasks;

import messages.*;
import akka.actor.*;


public class ChannelManager extends UntypedActor {

	private final String identifyId = "1";
	
	/* This variable is used to store the sender of GetOrCreateChannel message */
	private ActorRef storeSender;
	
	/* This is used to store channel name given in GetOrCreateChannel message */
	private String chName = null;
	
	@Override
	public void onReceive(Object msg) throws Exception {
		
		if (msg instanceof GetOrCreateChannel) {
			final String cName = ((GetOrCreateChannel) msg).name;
			chName = cName;
			storeSender = getSender();
			
			/* Check whether the channel with the name chName is created or not */
			ActorSelection checkChannel = context().actorSelection("/user/channels/" + chName);
			checkChannel.tell(new Identify(identifyId), self());
				
			
		} else if (msg instanceof ActorIdentity) {
			ActorIdentity identity = (ActorIdentity) msg; 
			if (identity.correlationId().equals(identifyId)) {
				ActorRef ref = identity.getRef();
				if (ref == null) {
					/* This means channel not exists
					 * Create channel as a child and send its reference to the sender of GetOrCreateChannel 
					 * */
					ActorRef child = context().actorOf(Props.create(Channel.class), chName);
					storeSender.tell(child, self());
				} else {
					/* This means that the channel exists
					 * The channel reference is sent to the sender of GetOrCreateChannel
					 * */
					storeSender.tell(ref, self());;
				}
			}
			
		} else
			unhandled(msg);
	}
}
