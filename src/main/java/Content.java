import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Thread;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.*;

class Content {
    
    private static String contentFile;
    private static int contentPort;
    private static int nameServerPort;
    private HashMap content;
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
	String regString = "register content localhost " + contentPort;
	sendData = regString.getBytes();
	regPacket.setData(sendData);

	waitForAck(serverSocket, regPacket, receivePacket);
	serverSocket.receive(receivePacket);
	String ret = new String(receivePacket.getData());

	System.out.println(ret);
	
	if (!(ret.contains("registered"))) {
	    System.err.println("Content registration with NameServer failed");
	    System.exit(1);
	} else {
	    // registered send ack
	    String ack = "ACK";
	    sendData = ack.getBytes();
	    regPacket.setData(sendData);
	    serverSocket.send(regPacket);
	}
    }
    
    /**
     * public boolean readStockFile()
     * Reads the stock file given via the command line arguments and
     * parses it's content.
     *
     * Post: Populates the content hashmap

     */
    public boolean readStockFile() {
	Charset charset = Charset.forName("US-ASCII");
	try {
	    // open and read each line of the content file
	    Path contentPath = Paths.get(contentFile);
	    BufferedReader buff =  Files.newBufferedReader(contentPath);
	    String line;
	    // parse each line and append it to the content hashmap
	    while ((line = buff.readLine()) != null) {
		String[] stockLine = line.split(" ");
		Long sNum = new Long(stockLine[0]);
		content.put(sNum, stockLine[1]);
	    }
		
	} catch (IOException e) {
	    // if the content file cannot be opened, shutdown the server
	    System.err.println("Invalid command line arguments for Content");
	    System.exit(1);
	    return false;
	}
	return true;
    }

    public Content() throws Exception {
	r = new Random();
	serverSocket = new DatagramSocket(contentPort);
	register();
	content = new HashMap<Long, String>();
	readStockFile();
	System.out.println("Content test");
	
	// arryays for receieveing and sending data via udp
	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];

	while(true) {
	    // Now have a socket to use for communication
	    // Create a PrintWriter and BufferedReader for interaction with our stream "true" means we flush the stream on newline
	    
	    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

	    sendData = "ACK".getBytes();
	    serverSocket.receive(receivePacket);

	    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
	    // send ack to store to ackowledge request
	    serverSocket.send(sendPacket); //(sendPacket, serverSocket, r);

	    String line = new String(receivePacket.getData());
	    System.out.println(line);
	    Long itemId = new Long(line.trim());
	    String contString = (String)content.get(itemId);
	    System.out.println(itemId + " " + contString);
	    
	    sendData = null;
	    sendData = new byte[1024];

	    sendData = contString.getBytes();
	    sendPacket.setData(sendData);
	    waitForAck(serverSocket, sendPacket, receivePacket);

	    sendData = null;
	    sendData = new byte[1024];
	    System.out.println("Content request processed");
	}
	
    }
    
    public static void main(String args[]) throws Exception {
	
	// Parse arguments
	if (args.length != 3) {
	    System.err.println("Invalid command line argument for Content");
	    System.exit(0);
	}
	// Assign arguments to globals
	contentPort = Integer.parseInt(args[0]);
	contentFile = args[1];
	nameServerPort = Integer.parseInt(args[2]);

	try {
	    // Start content server
	    new Content();

	} catch (IOException e) {
	    e.printStackTrace();
	}
	
    }
    
}
