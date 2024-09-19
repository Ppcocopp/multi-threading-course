package info5.sar.events.queues;

import info5.sar.events.channels.Broker.AcceptListener;
import info5.sar.events.channels.Broker.ConnectListener;
import info5.sar.events.channels.Channel;
import info5.sar.events.channels.Channel.ReadListener;
import info5.sar.events.channels.Channel.WriteListener;
import info5.sar.utils.Executor;

public class Test {
	
	static boolean finished = false;
	static int written=0, read=0;

	public static void main(String[] args) {
		Executor executor = new Executor("Executor");
		executor.start();
		CBroker brokerClient = new CBroker("Client", executor);
		CBroker brokerServeur = new CBroker("Serveur", executor);
		int messageSize = 50;
		byte[] message = new byte[messageSize];
		for(int i=0; i<messageSize; i++) {
			message[i] = (byte) i;
		}
		byte[] receivedMessage = new byte[messageSize];
		brokerClient.connect("Serveur", 80, new ConnectListener() {
			@Override
			public void connected(Channel channel){
				try {
					channel.write(message, 0, messageSize, new WriteListener() {
						
						@Override
						public void written(byte[] bytes, int offset, int length, int written) {
							if(written+Test.written<messageSize) {
								Test.written+=written;
								try {
									channel.write(message, Test.written, messageSize-Test.written, this);
								}catch(Exception e) {
									
								}
							}else {
								System.out.println("Message written");
							}
						}
					});
				}catch (Exception e){
					
				}
			}
		});
		brokerServeur.accept(80, new AcceptListener() {
			
			@Override
			public void accepted(Channel channel) {
				try {
					channel.read(receivedMessage, 0, messageSize, new ReadListener() {
						
						@Override
						public void read(byte[] bytes) {
							if(bytes.length+Test.read==messageSize) {
								System.out.println("Message received");
								Test.finished = true;
							}else {
								Test.read += bytes.length;
								try {
									channel.read(receivedMessage, Test.read, messageSize-Test.read, this);
								}catch (Exception e) {
									
								}
								
							}
						}
					});
				}catch (Exception e) {
					
				}
				
			}
		});
		
		while(!finished) {
			try {
				Thread.sleep(1000);
				System.out.println("Waiting");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
