import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Thread;

class NameServer {
    
    /**
     * Hosts is Hashmap with type <String, String[]>
     * The key of the hashmap is the server name, the value for the hashmap
     * is a 2 
     */
    private HashMap hosts;

    // port number of the name server
    public static int portNum;
    public Random r;

    /**
     * sendPacket(DatagramPacket packet, DatagramSocket, sock, Random r)
     *  This is responsible for simulating the packet loss of a UDP connection
     *  It will keep looping until the packet is send to its destination.
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

    public ArrayList parseMessage(String message) {
	// Arraylist toe return the data to the client or server.
	ArrayList ret = new ArrayList<String>();
	// tokenized version of the message from the sender
	String[] tokens = message.toLowerCase().trim().split(" ");
	 
	// Switch statment which handles the queries from the sender, it the query
	// isn't a register or lookup query then they are returned an error
	switch (tokens[0]) {
	case "lookup":

	    // Incorrect message format
	    if (tokens.length != 2) {
		break;
	    }	

	    // check lookup query for the correct format and check the database for the entry
	    String query = tokens[1].toLowerCase().trim();
	    // If the process is registered with the server then return the address and port for the sender
	    if (hosts.containsKey(query)) {
		String val[] = (String[])hosts.get(query);
		ret.add(val[0] + " " + val[1] + "\n");
		return ret;
		// if the process isn't registered then return the appropriate error
	    } else {
		ret.add(0, "Error: Process has not registered with the Name Server\n");
		return ret;
	    }
	    
	case "register":
	    // Incorrect register message
	    if (tokens.length != 4) {
		ret.add("error\n");
		return ret;
	    } else {
		String name;
		String[] details = new String[2];
		name = tokens[1];
		details[0] = tokens[2];
		details[1] = tokens[3];
		// check if the port being provided is a reserved port
		if (Integer.parseInt(tokens[3]) < 1024) {
		    ret.add("error\n");
		    return ret;
		}
		hosts.put(name, details);
		
		ret.add("registered\n");

		return ret;
	    }
	}
	// Default option is to return an error to the sender, this will close the connection
	ret.add("error\n");
	return ret;
    }
    
    public NameServer() throws Exception{
	r = new Random();
	hosts = new HashMap<String, String[]>();

	// create socket for name server
	DatagramSocket serverSocket = new DatagramSocket(portNum);
	
	// set buffers
	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];
	System.out.println("<UDPServer> Server is activated.");

	while (true) {
	    // receive message from client
	    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	    serverSocket.receive(receivePacket);
	    // packet recieved, send ACK
	    // get the port of the client
	    InetAddress IPAddress = receivePacket.getAddress();
	    int port = receivePacket.getPort();
	    System.out.println(port);
	    // send ack to sender
	    String ack = "ACK";
	    sendData = ack.getBytes();
	    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
	    serverSocket.send(sendPacket);
	    
	    // print the message
	    String msg = new String(receivePacket.getData());
	    
	    System.out.println("Message from Client: " + msg);

	    //process the message from the sender
	    ArrayList ret = parseMessage(msg.trim());
	    
	    sendData = ret.toString().getBytes();
	    sendPacket.setData(sendData);
	    // send the message back to the client 
	    // serverSocket.send(sendPacket);
	    waitForAck(serverSocket, sendPacket, receivePacket);
	    receiveData = null;
	    receiveData = new byte[1024];
	}
    }

    public static void main(String args[]) throws Exception {
	// Ensure command line arguments are in the correct format
	if (args.length != 1) {
	    System.err.println("Invalid command line arguments for NameServer");
	    System.exit(1);
	}

	// parse port number
	portNum = Integer.parseInt(args[0]);
	new NameServer();
	
    }
    
}
