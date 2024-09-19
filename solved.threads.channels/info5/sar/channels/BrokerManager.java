package info5.sar.channels;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows to manage multiple {@link Broker}. Must be unique to keep unicity of
 * Brokers name.
 */
public class BrokerManager {

	// map giving the Broker from its name
	private static Map<String, CBroker> brokers = new HashMap<>();

	/**
	 * @param broker : Broker to store
	 * @throws IllegalArgumentException if the name of the broker is not unique
	 */
	public synchronized static void addBroker(CBroker broker) throws IllegalArgumentException {
		if (isNameUsed(broker.getName()))
			throw new IllegalArgumentException("Broker name not unique (" + broker.getName() + ")");
		brokers.put(broker.getName(), broker);
	}

	/**
	 * @param name : name of the Broker
	 * @return true if the name is already used
	 */
	public static boolean isNameUsed(String name) {
		return brokers.containsKey(name);
	}

	/**
	 * @param name : name of the Broker
	 * @return The Broker if found, null otherwise
	 */
	public static CBroker getBroker(String name) {
		return brokers.get(name);
	}

}
