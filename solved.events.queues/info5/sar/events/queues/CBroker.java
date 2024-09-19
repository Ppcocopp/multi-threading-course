package info5.sar.events.queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info5.sar.events.channels.Broker;
import info5.sar.utils.Executor;

public class CBroker extends Broker {
	
	// map storing the accept listeners on accepting ports
	private Map<Integer, AcceptListener> acceptingPorts = new HashMap<>();
	// map storing connect listeners waiting for an accept on the ports
	private Map<Integer, List<ConnectListener>> connectingPorts = new HashMap<>();
	// the event executor pump
	private Executor executor;

	public CBroker(String name, Executor executor) {
		super(name);
		BrokerManager.addBroker(this);
		this.executor = executor;
	}

	/*
	 * If the acceptingPorts map is storing an AcceptListeler on the given port, throw a IllegalArgumentException
	 * If the connectingPorts map has listener(s) on the given port :
	 * - retrieve the first ConnectListener of the list and remove it from the list.
	 * - create 2 new linked CChannels
	 * - create a new Runnable calling the accepted() method of the given listener with one of the channels from its run() method.
	 * - create another new Runnable calling the connected() method of the retrieved ConnectListener with the other channel from its run() method.
	 * - post these 2 Runnable to the executor
	 * Else :
	 * - Add the given listener to the acceptingPorts map on the given port
	 */
	@Override
	public synchronized void accept(int port, AcceptListener listener) {
		if(acceptingPorts.get(port)!=null) throw new IllegalArgumentException(this.toString()+" accept : invalid port");
		if(connectingPorts.get(port)!=null && connectingPorts.get(port).size()>0) {
			ConnectListener connectListener = connectingPorts.get(port).get(0);
			connectingPorts.get(port).remove(connectListener);
			if(connectingPorts.get(port).size()==0) connectingPorts.remove(port);
			CChannel acceptChannel = new CChannel(this, port, executor);
			CChannel connectChannel = new CChannel(this, port, acceptChannel, executor);
			Runnable runnableAccept = new Runnable() {
				@Override
				public void run() {
					listener.accepted(acceptChannel);
				}
			};
			Runnable runnableConnect = new Runnable() {
				@Override
				public void run() {
					connectListener.connected(connectChannel);
				}
			};
			executor.post(runnableAccept);
			executor.post(runnableConnect);
		} else {
			acceptingPorts.put(port, listener);
		}
	}

	/*
	 * Retrieve the distant broker and return false if it's null.
	 * In a synchronized block on the retrieved broker object :
	 * | If the acceptingPorts map is storing an AcceptListeler on the given port :
	 * | - retrieve the AcceptListener of the distant map and remove it after
	 * | - create 2 new linked CChannels
	 * | - create a new Runnable calling the accepted() method of the retrieved AcceptListener with one of the channels from its run() method.
	 * | - create another new Runnable calling the connected() method of the given listener with the other channel from its run() method.
	 * | - post these 2 Runnable to the executor
	 * | Else :
	 * | - If the distant ConnectListener list in the map is not initialized, initialize it
	 * | - Add the given listener to the lift of the distant map on the given port
	 * Return true
	 */
	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		CBroker broker = BrokerManager.getBroker(name);
		if(broker==null) return false;
		synchronized(broker) {
			if(broker.acceptingPorts.get(port)!=null) {
				AcceptListener acceptListener = broker.acceptingPorts.get(port);
				broker.acceptingPorts.remove(port);
				CChannel connectChannel = new CChannel(this, port, executor);
				CChannel acceptChannel = new CChannel(this, port, connectChannel, executor);
				Runnable runnableConnect = new Runnable() {
					@Override
					public void run() {
						listener.connected(connectChannel);
					}
				};
				Runnable runnableAccept = new Runnable() {
					@Override
					public void run() {
						acceptListener.accepted(acceptChannel);
					}
				};
				executor.post(runnableConnect);
				executor.post(runnableAccept);
			}else {
				if(broker.connectingPorts.get(port)==null) {
					ArrayList<ConnectListener> list = new ArrayList<>();
					broker.connectingPorts.put(port, list);
				}
				broker.connectingPorts.get(port).add(listener);
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "[CBroker "+this.getName()+"]";
	}


}
