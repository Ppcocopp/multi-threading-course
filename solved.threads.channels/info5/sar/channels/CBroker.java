package info5.sar.channels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link Broker}
 */
public class CBroker extends Broker {

	// map storing the list of rendez-vous from their port
	private Map<Integer, List<RDVChannelConnexion>> rdvs = new HashMap<>();

	/*
	 * Call the super() constructor with the given name.
	 * Add this Broker to the BrokerManager.
	 */
	/**
	 * @param name : name of the Broker
	 * @throws IllegalArgumentException if the name is not unique
	 * @see {@link Broker#Broker(String) Broker(String)}
	 */
	public CBroker(String name) {
		super(name);
		BrokerManager.addBroker(this);

	}

	/*
	 * (In this part RDVChannelConnexion will be told rendez-vous)
	 * Create a null List variable to store a rendez-vous list.
	 * In a synchronized block on the map object in field :
	 * | If the map has no list (null) for the given port, create a new empty one.
	 * | Retrieve the rendez-vous list corresponding to the given port.
	 * Create a null rendez-vous variable.
	 * In a synchronized block on the retrieved list object :
	 * | Iterate on every rendez-vous of the list and throw a IllegalArgumentException if it already has an accept.
	 * | If the list is empty : 
	 * | - create a new rendez-vous and store it in the previous variable.
	 * | - add it to the list.
	 * | - accept() the rendez-vous.
	 * | Else :
	 * | - store the first rendez-vous of the list in the previous variable.
	 * | - accept() it.
	 * | - create a CChannel (acceptChannel)
	 * | - create a second CChannel (connectChannel) from the acceptChannel
	 * | - setChannel() to the rendez-vous with the connectChannel
	 * | - remove the rendez-vous from the list
	 * | - join() the rendez-vous
	 * | - return the acceptChannel
	 * Join the rendez-vous.
	 * Return the CChannel from of the rendez-vous
	 */
	@Override
	public Channel accept(int port) {
		// retrieve the rendez-vous list of the given port
		List<RDVChannelConnexion> rendezVous;
		synchronized(rdvs) {
			if(rdvs.get(port)==null) {
				rdvs.put(port, new ArrayList<>());
			}
			rendezVous = rdvs.get(port);
		}
		// retrieve the rendez-vous
		RDVChannelConnexion rdv;
		synchronized(rendezVous) {
			for(RDVChannelConnexion r : rendezVous)
				if(r.hasAccept())
					throw new IllegalArgumentException(this.toString()+" accept : Invalid port");
			if(rendezVous.isEmpty()) {
				rdv = new RDVChannelConnexion();
				rendezVous.add(rdv);
				rdv.accept(this);
			}else {
				rdv = rendezVous.get(0);
				rdv.accept(this);
				// create the connection and leave the other part in the rendez-vous for the connect()
				CChannel acceptChannel = new CChannel(this, port);
				rdv.setChannel(new CChannel(rdv.getDistantBroker(), port, acceptChannel));
				rendezVous.remove(rdv);
				rdv.join();
				return acceptChannel;
			}
		}
		rdv.join();
		return rdv.getChannel();
	}

	/*
	 * (In this part RDVChannelConnexion will be told rendez-vous)
	 * Retrieve the CBroker from the BrokerManager with the given name.
	 * If the retrieved CBroker is null, return null.
	 * Create a null List variable to store a rendez-vous list.
	 * In a synchronized block on the map object in field :
	 * | If the map has no list (null) for the given port, create a new empty one.
	 * | Retrieve the rendez-vous list corresponding to the given port.
	 * Create a null rendez-vous variable.
	 * In a synchronized block on the retrieved list object :
	 * | Iterate on every rendez-vous of the list and retrieve the first one with an accept.
	 * | If no rendez-vous has been found : 
	 * | - create a new rendez-vous and store it in the previous variable.
	 * | - add it to the list.
	 * | - connect() the rendez-vous.
	 * | Else :
	 * | - store the first rendez-vous of the list in the previous variable.
	 * | - connect() it.
	 * | - create a CChannel (connectChannel)
	 * | - create a second CChannel (acceptChannel) from the connectChannel
	 * | - setChannel() to the rendez-vous with the connectChannel
	 * | - remove the rendez-vous from the list
	 * | - join() the rendez-vous
	 * | - return the connectChannel
	 * Join the rendez-vous.
	 * Return the CChannel from of the rendez-vous
	 */
	@Override
	public Channel connect(String name, int port) {
		// retrieve the 'distant' CBroker
		CBroker broker = BrokerManager.getBroker(name);
		if (broker == null) {
			return null;
		}
		// retrieve the rendez-vous list of the given port
		List<RDVChannelConnexion> rendezVous;
		synchronized(broker.rdvs) {
			if(broker.rdvs.get(port)==null) {
				broker.rdvs.put(port, new ArrayList<>());
			}
			rendezVous =broker.rdvs.get(port);
		}
		// retrieve the rendez-vous
		RDVChannelConnexion rdv = null;
		synchronized(rendezVous) {
			for(RDVChannelConnexion r : rendezVous) {
				if(r.hasAccept()) {
					rdv = r;
					break;
				}
			}
			if(rdv==null) {
				rdv = new RDVChannelConnexion();
				rendezVous.add(rdv);
				rdv.connect(this);
			} else {
				rdv = rendezVous.get(0);
				rdv.connect(this);
				// create the connection and leave the other part in the rendez-vous for the accept()
				CChannel connectChannel = new CChannel(this, port);
				rdv.setChannel(new CChannel(rdv.getDistantBroker(), port, connectChannel));
				rendezVous.remove(rdv);
				rdv.join();
				return connectChannel;
			}
		}
		rdv.join();
		return rdv.getChannel();
	}
	
	@Override
	public String toString() {
		return "[CBroker "+name+"]";
	}

}
