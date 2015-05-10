import java.io.*;
import java.net.*;
import java.util.Random;

public class Client {
    public DatagramSocket serverSocket;
    public Random r;
    public String storeAdd;
    /**
     * sendPacket(DatagramPacket packet, DatagramSocket sock, Random r)
     * Sends a packet to a specific address, uses random chance to simulate packet loss
     */
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

    public String getStoreAddress(int nameServerPort)
	throws Exception {
	
	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];
	
	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), nameServerPort);
	
	// contruct registration string and sernd itto nameserver
	String searchString = "lookup store";
	sendData = searchString.getBytes();
	sendPacket.setData(sendData);
	waitForAck(serverSocket, sendPacket, receivePacket);
	
	serverSocket.receive(receivePacket);
	
	String ack = "ACK";
	sendData = ack.getBytes();
	sendPacket.setData(sendData);
	serverSocket.send(sendPacket);
	
	String ret = new String(receivePacket.getData());
	
	if (ret.trim().toLowerCase().contains("error")) {
	    System.err.println("Client was unable to connect to Store");
	    System.exit(1);
	} 

	return ret;
    }
    
    public Client(int request, int nameServerPort) throws Exception {
	r = new Random();
	serverSocket = new DatagramSocket();
	storeAdd = getStoreAddress(nameServerPort);
	System.out.println(storeAdd);
	System.out.println("Client");

	// send actual request to the store
	InetAddress storeAddress = InetAddress.getByName(storeAdd.split(" ")[0].replace("[", "").trim());
	int storePort = Integer.parseInt(storeAdd.split(" ")[1].replace("]", "").trim());

	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];
	String query = "";
	
	if (request == 0 ) {
	    query = "lookup";
	} else if (request == 1) {
	    query = "purchase " + request + " 4321432143214321";
	}
	sendData = query.getBytes();
	
	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, storeAddress, storePort);

	waitForAck(serverSocket, sendPacket, receivePacket);
	serverSocket.receive(receivePacket);
	
	String ack = "ACK";
	sendData = ack.getBytes();
	sendPacket.setData(sendData);
	serverSocket.send(sendPacket);

	System.out.println(new String(receivePacket.getData()));
	
    } 
    
    public static void main(String args[]) throws Exception {
	// parse command line arguments
	if (args.length != 2) {
	    System.err.println("Invalid command line arguments");
	    System.exit(0);
	}

	// parse command line arguments into globals
	int request  = 0;
	int nameServerPort = 0; 
	try {
	    request = Integer.parseInt(args[0]);
	    nameServerPort = Integer.parseInt(args[1]);
	} catch (Exception e) {
	    System.err.println("Invalid command line arguments");
	    System.exit(1);
	}
	// ensure the request is valid
	if (request > 10 || request < 0) {
	    System.err.println("Invalid command line arguments");
	    System.exit(0);
	}
	
	Client client = new Client(request, nameServerPort);
    }

}
