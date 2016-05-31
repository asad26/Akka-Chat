package tasks;

import java.util.LinkedList;
import java.util.List;

import messages.*;
import akka.actor.*;

public class Channel extends UntypedActor {

	/* This variable is declared for storing users (joined to the channel) in a list */
	private final List<ActorRef> users = new LinkedList<ActorRef>();
	
	/* This variable is declared for storing messages in a list */
	private final List<ChatMessage> allMessages = new LinkedList<ChatMessage>();
	
	/* Return the name of channel */
	private String getChannelName() {
		return self().path().name();
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		
		if (msg instanceof ChatMessage) {
			final ChatMessage message = (ChatMessage) msg;
			allMessages.add(message);
			
			/* Then the message is sent to all connected users of this channel,
			 * as an instance of Backlog message 
			 * */
			for (ActorRef u : users) {
				u.tell(new Backlog(this.getChannelName(), message), self());
			}
			
		} else if (msg instanceof AddUser) {
			ActorRef aUser = ((AddUser) msg).user;
			
			/* This shows that the user has joined that channel */
			users.add(aUser);
			
			/* Reply to the actor by sending an instance of UserAdded message
			 * Also send the complete list of actor as instance of Backlog message 
			 * */
			aUser.tell(new UserAdded(this.getChannelName(), self()), self());
			aUser.tell(new Backlog(this.getChannelName(), allMessages), self());
			
			/* The user is registered for death watch */
			context().watch(aUser);

		} else if (msg instanceof RemoveUser) {
			ActorRef rUser = ((RemoveUser) msg).user;
			users.remove(rUser);
			
			/* Reply to the removed actor by sending an instance of UserRemoved message */
			rUser.tell(new UserRemoved(this.getChannelName(), self()), self());
			
		/* The terminated user/actor is removed from the list */	
		} else if (msg instanceof Terminated) {
			Terminated t = (Terminated) msg;
			if (users.contains(t.getActor())) {
				users.remove(t.actor());
			}
			
		} else
			unhandled(msg);
	}
}