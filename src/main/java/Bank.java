import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Thread;
import java.net.InetAddress;

class Bank {
    
    private static int bankPort;
    private static int nameServerPort;
    public DatagramSocket serverSocket;
    public Random r;

    public boolean sendPacket(DatagramPacket packet, DatagramSocket sock, Random r) 
	throws Exception {
	float transmit = r.nextFloat();
	if (transmit > 0.5) {
	    // transmit packet
	    sock.send(packet);
	    System.out.println("sent");
	    return true;
	}
	System.out.println("not sent");
	return false;
    }

    /**
     * public void waitForAck(DatagramSocket sock, DatagramPacket send, DatagramPacket recv) 
     */
    public void waitForAck(DatagramSocket sock, DatagramPacket send, DatagramPacket recv)
	throws Exception {
	
	sock.setSoTimeout(3000);	
	sendPacket(send, sock, r);
	boolean ackd = false;
	byte[] recvData  = new byte[1024];
	recv.setData(recvData);
	while (!(ackd)) {
	    // simulate packet loss
	    try {
		sock.receive(recv);
		String ack = new String(recv.getData());
		System.out.println("Ack? "+ ack);
		if (ack.trim().equals("ACK")) {
		    System.out.println("acked");
		    ackd = true;
		    break;
		}
		recvData  = null;
		recvData = new byte[1024];
		recv.setData(recvData);

	    } catch (SocketTimeoutException e) {
		sendPacket(send, sock, r);
	    }
	}
	sock.setSoTimeout(0);	
    }
    
    public void register() throws Exception {
	System.out.println(serverSocket);
	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];
	
	DatagramPacket regPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), nameServerPort);
	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	
	// contruct registration string and sernd itto nameserver
	String regString = "register bank localhost " + bankPort;
	sendData = regString.getBytes();
	regPacket.setData(sendData);

	waitForAck(serverSocket, regPacket, receivePacket);
	serverSocket.receive(receivePacket);
	String ret = new String(receivePacket.getData());

	System.out.println(ret);
	
	if (!(ret.contains("registered"))) {
	    System.err.println("Bank registration with NameServer failed");
	    System.exit(1);
	} else {
	    // registered send ack
	    String ack = "ACK";
	    sendData = ack.getBytes();
	    regPacket.setData(sendData);
	    serverSocket.send(regPacket);
	}
	
    }

    public Bank() throws Exception {
	serverSocket = new DatagramSocket(bankPort);
	r = new Random();
	register();
	System.out.println("Bank Test");

	// arryays for receieveing and sending data via udp
	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];
	
	while(true) {

	    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

	    sendData = "ACK".getBytes();
	    
	    serverSocket.receive(receivePacket);

	    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());

	    // send ack to store to ackowledge request
	    serverSocket.send(sendPacket); //(sendPacket, serverSocket, r);
	    
	    String line = new String(receivePacket.getData());
	    
	    Long itemId = new Long(line.trim().split(" ")[1]);

	    // check if the process with succeed or not. 
	    // if the id is even, success, otherwise fail.
	    if (itemId % 2 != 0) {
		line = "1";
		System.out.println(itemId + " OK");
	    } else {
		line = "0";
		System.out.println(itemId + " NOT OK");
	    }

	    sendData = null;
	    sendData = new byte[1024];
	    sendData = line.getBytes();
	    sendPacket.setData(sendData);
	    waitForAck(serverSocket, sendPacket, receivePacket);
	    System.out.print("Purchase request processed");
	}
    }
    
    public static void main(String args[]) throws IOException {

	// Command line argument error checking
	if (args.length != 2) {
	    System.err.println("Invalid command line arguments for Bank");
	    System.exit(1);
	}

	// parse the command line arguments into the globals
	bankPort = Integer.parseInt(args[0]);
	nameServerPort = Integer.parseInt(args[1]);

	// start the bank server
	try {
	    new Bank();
	} catch (Exception e ) {
	    e.printStackTrace();
	}
    }
}
