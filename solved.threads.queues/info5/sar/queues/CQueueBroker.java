package info5.sar.queues;

import info5.sar.channels.Broker;
import info5.sar.channels.CBroker;
import info5.sar.channels.Channel;

public class CQueueBroker extends QueueBroker {
	
	// the channel used for communication
	Channel channel;
	// the child MessageQueue
	MessageQueue messageQueue;
	
	/**
	 * @param broker : the Broker to layout
	 */
	public CQueueBroker(Broker broker){
		super(broker);
	}

	/*
	 * Set the channel field with the result of accept() of the broker field
	 * Set the messageQueue field with a new CMessageQueue created from the channel field
	 * Return the messageQueue field
	 */
	@Override
	public MessageQueue accept(int port) {
		channel = broker.accept(port);
		messageQueue = new CMessageQueue(channel, this);
		return messageQueue;
	}

	/*
	 * Set the channel field with the result of connect() of the broker field
	 * Set the messageQueue field with a new CMessageQueue created from the channel field
	 * Return the messageQueue field
	 */
	@Override
	public MessageQueue connect(String name, int port) {
		channel = broker.connect(name, port);
		messageQueue = new CMessageQueue(channel, this);
		return messageQueue;
	}

}
