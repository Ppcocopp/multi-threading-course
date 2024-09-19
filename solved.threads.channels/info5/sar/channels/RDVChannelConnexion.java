package info5.sar.channels;

/**
 * This object is used to manage the rendez-vous between 2 threads.<br>
 * <br>
 * It stores a CBroker from the 1st thread for the 2nd one.<br>
 * It stores a CChannel from the 2nd thread for the 1st one.<br>
 * <br>
 * You can know if this rendez-vous has already been connected or accepted and can join the rendez-vous point. 
 */
public class RDVChannelConnexion {
	
	// flags to know if the accept() and connect() methods have been called
	private boolean hasConnect=false, hasAccept=false;
	// the channel to store for the 1st thread thread
	private CChannel channel;
	// the meeting point
	private RDV rdv = new RDV();
	//the broker to store for the 2nd thread
	private CBroker distantBroker;
	
	/*
	 * Set the hasAccept flag to true
	 * Set the distantBroker field to the given broker
	 */
	/**
	 * @param broker : the broker of the accepting channel to store
	 */
	public synchronized void accept(CBroker broker) {
		hasAccept = true;
		distantBroker = broker;
	}
	
	/*
	 * Set the hasConnect flag to true
	 * Set the distantBroker field to the given broker
	 */
	/**
	 * @param broker : the broker of the connecting channel to store
	 */
	public synchronized void connect(CBroker broker) {
		hasConnect = true;
		distantBroker = broker;
	}
	
	/*
	 * Call the join() method of the meeting point in field
	 */
	/**
	 * Joins the blocking rendez-vous.
	 */
	public void join() {
		rdv.join();
	}
	
	/*
	 * Return the value of the hasAccept flag
	 */
	/**
	 * @return True if the {@link #accept(CBroker)} method has been called
	 */
	public synchronized boolean hasAccept() {
		return hasAccept;
	}
	
	/*
	 * Return the value of the hasConnect flag
	 */
	/**
	 * @return True if the {link {@link #connect(CBroker)} method has been called
	 */
	public synchronized boolean hasConnect() {
		return hasConnect;
	}
	
	/*
	 * Return the channel field
	 */
	/**
	 * @return The stored CChannel
	 */
	public CChannel getChannel() {
		return channel;
	}

	/*
	 * Set the channel field with the given channel
	 */
	/**
	 * @param channel : the channel to store
	 */
	public void setChannel(CChannel channel) {
		this.channel = channel;
	}

	/*
	 * Return the distantBroker field
	 */
	/**
	 * @return The stored CBroker
	 */
	public CBroker getDistantBroker() {
		return distantBroker;
	}

	/**
	 * Meeting point for 2 threads
	 * (thread-safe blocking rendez-vous)
	 */
	private class RDV {
		// number of joined threads
		int threads = 0;
		
		/*
		 * Increment the threads field by 1.
		 * While the threads fields is inferior to 2 :
		 * - wait() on this object and do nothing if a InterruptedException occurs.
		 * Notify a thread on this object.
		 */
		/**
		 * Joins the blocking rendez-vous.
		 * This method is unblocking when the 2nd thread joins
		 */
		public synchronized void join() {
			threads++;
			while(threads<2) {
				try {
					wait();
				} catch (InterruptedException e) {
					// Nothing to do here
				}
			}
			notify();
		}
	}

}
