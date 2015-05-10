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

class Store {
    
    // HashMap holding the content numbers and their prices
    private HashMap content;
    
    //Port that the store server will listen on
    private static int stockPort;
    // Port for connecting to the nameserver
    private static int nameServerPort;
    // File containing content
    private static String stockFile;

    private String bankAdd;
    private String contentAdd;
    
    public DatagramSocket serverSocket;
    public Random r;

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

    public void register() throws Exception {

	System.out.println(serverSocket);
	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];
	
	DatagramPacket regPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(), nameServerPort);
	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	
	// contruct registration string and sernd itto nameserver
	String regString = "register store localhost " + stockPort;
	sendData = regString.getBytes();
	regPacket.setData(sendData);

	waitForAck(serverSocket, regPacket, receivePacket);
	serverSocket.receive(receivePacket);
	String ret = new String(receivePacket.getData());

	System.out.println(ret);
	
	if (!(ret.contains("registered"))) {
	    System.err.println("Store registration with NameServer failed");
	    System.exit(1);
	} else {
	    // registered send ack
	    String ack = "ACK";
	    sendData = ack.getBytes();
	    regPacket.setData(sendData);
	    serverSocket.send(regPacket);
	}

	// try and get the bank and content server addresses
	receiveData = null;
	receiveData = new byte[1024];
	sendData = null;
	sendData = new byte[1024];
	sendData = "lookup bank".getBytes();
	regPacket.setData(sendData);
	receivePacket.setData(receiveData);
	waitForAck(serverSocket, regPacket, receivePacket);

	serverSocket.receive(receivePacket);
	
	bankAdd = new String(receivePacket.getData());
	
	System.out.println("bank: " + bankAdd);
	
	if (bankAdd.contains("Error")) {
	    System.err.println("Bank has not registered");
	    System.exit(1);
	}
	
	receiveData = null;
	receiveData = new byte[1024];
	sendData = null;
	sendData = new byte[1024];
	sendData = "lookup content".getBytes();
	
	String ack = "ACK";
	sendData = ack.getBytes();
	regPacket.setData(sendData);
	
	receivePacket.setData(receiveData);
	serverSocket.send(regPacket);
	
	sendData = null;
	sendData = new byte[1024];
	sendData = "lookup content".getBytes();
	regPacket.setData(sendData);
	waitForAck(serverSocket, regPacket, receivePacket);
	
	serverSocket.receive(receivePacket);
	
	contentAdd = new String(receivePacket.getData());
	System.out.println("Content: " + contentAdd);
	
	if (contentAdd.contains("Error")) {
	    System.err.println("Content has not registered");
	    System.exit(1);
	}
	
	ack = "ACK";
	sendData = ack.getBytes();
	regPacket.setData(sendData);
	serverSocket.send(regPacket);
	
	receiveData = null;
	receiveData = new byte[1024];
	// Close socket connections with nameserver
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
	// read lines from the content file
	try {
	    Path stock = Paths.get(stockFile);
	    BufferedReader buff =  Files.newBufferedReader(stock);
	    String line;
	    // loop ove the lines in the content file and append them to the content hashmap
	    while ((line = buff.readLine()) != null) {
		String[] stockLine = line.split(" ");
		Long sNum = new Long(stockLine[0]);
		content.put(sNum, Float.parseFloat(stockLine[1]));
	    }
	} catch (IOException e) {
	    // catch trying to open an invalid file
	    System.err.println("Invalid command line arguments for Store");
	    System.exit(1);
	    return false;
	}
	return true;
    }
    
/**
 * parseMessage(String message)
 * 
 * Parses the messages recieved by the Store server. 
 * This server will only ever recieve messages for either looking up the 
 * stores contents or for purchasing content.
 *
 * @param message String representation of the message recieved
 * @return The parsed message that will be sent back to the client
 */
    public String parseMessage(String message) throws Exception {

	// Parse a lookup message, simply return the content of the store to the client
	if (message.toLowerCase().trim().equals("lookup")) {
	    String ret = "";
	    int count = 1;
	    for (Object key : content.keySet()) {
		ret = ret + count + ". " + (Long)key + " " + content.get(key) + "\n";
		count++;
	    }
	    return ret;
	} else if (message.toLowerCase().trim().contains("purchase")) {
	    // Purchase request recieved. 

	    //split the message to get the id number
	    String idString = message.trim().split(" ")[1];
	    String ccNum = message.trim().split(" ")[2];

	    // Return an error if the credit card is not of the correct length
	    if (ccNum.length() != 16) {
		return "Error";
	    }
	    
	    // Minus 1 to account for array index numbering
	    int id = Integer.parseInt(idString) - 1;
	    // Ensure the id is within range of the content files hashmap
	    if (id > 9 || id < 0) {
		System.out.println("error");
	    }

	    // get the item id and item price from the stock hash
	    Object[] ids = content.keySet().toArray();
	    Long itemId = (Long)ids[id];
	    Float itemPrice = (Float)content.get(itemId);

	    // itemId, itemPrice and ccNum have been found, contact the bank
	    String outcome = messageBank(itemId, itemPrice, ccNum);
	    System.out.println(outcome);
	    
	    byte[] receiveData = new byte[1024];
	    byte[] sendData = new byte[1024];
	    
	    if (outcome.trim().equals("1")) {
	    	// message content server
		InetAddress contentAddress = InetAddress.getByName(contentAdd.split(" ")[0].replace("[", "").trim());
		int contentPort = Integer.parseInt(contentAdd.split(" ")[1].replace("]", "").trim());

		sendData = itemId.toString().getBytes();
		DatagramPacket contentPacket = new DatagramPacket(receiveData, receiveData.length);
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, contentAddress, contentPort);
		
		waitForAck(serverSocket, sendPacket, contentPacket);

		receiveData = null;
		receiveData = new byte[1024];
		contentPacket.setData(receiveData);
		serverSocket.receive(contentPacket);
		
		sendData = null;
		sendData = new byte[1024];
		String ack = "ACK";
		sendData = ack.getBytes();
		sendPacket.setData(sendData);
		serverSocket.send(sendPacket);
		
		// contentServer.out.println(itemId.toString());
	    	String contentRet = new String(contentPacket.getData()).trim();

		try {
		    // contentRet = contentServer.in.readLine();
		    // if the content server returned the correct response, send it to the user
		    if (contentRet.length() > 1) {
			String contentResponse = itemId + " ($ " + itemPrice +") CONTENT " + contentRet + "\n";
			// item-id ($ item-price) CONTENT item-content\n
			return contentResponse;
			// return contentRet + "\n";
		    }
		} catch (Exception e) {
		    // something went wrong
		}

		//otherwise send the aborted message
		String aborted = itemId + " \"transaction aborted\"\n";
		return aborted;
	    } else {
		// something went wrong, send the transaction aborted mesasge
		// add one to get back to the original id number
		String aborted = itemId + " \"transaction aborted\"\n";
		return aborted;
	    }
	    
	} else {
	}
	
	return "";
    }
    
/**
 * Message the bank and return the banks response.  
 * The parameters taken by this function have already been checked for errors
 * by the parseMessage function.
 * @param itemId - Long id of the file to be purchased
 * @param itemPrice - float representing the price of the item
 * @param ccNum - string representation of the users credit card number
 * @return If the bank accepts the transaction then return true, otherwise false.
 */
public String messageBank(Long itemId, Float itemPrice, String ccNum)
    throws Exception {
    byte[] receiveData = new byte[1024];
    byte[] sendData = new byte[1024];
	
    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    //construct and send the bank the purchase request
    String bankRequest = "purchase " + itemId.toString() + " " + itemPrice + " " + ccNum;
    sendData = bankRequest.getBytes();

    InetAddress bankAddress = InetAddress.getByName(bankAdd.split(" ")[0].replace("[", "").trim());
    int bankPort = Integer.parseInt(bankAdd.split(" ")[1].replace("]", "").trim());
    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, bankAddress, bankPort );
    waitForAck(serverSocket, sendPacket, receivePacket);

    receiveData = null;
    receiveData = new byte[1024];
    receivePacket.setData(receiveData);
    serverSocket.receive(receivePacket);

    sendData = null;
    sendData = new byte[1024];
    String ack = "ACK";
    sendData = ack.getBytes();
    sendPacket.setData(sendData);
    serverSocket.send(sendPacket);

    String retString = new String(receivePacket.getData()).trim();

    System.out.println("retstring" + retString);
	
    return retString;
}
    
public Store() throws Exception {
    r = new Random();
    content = new HashMap<Long, Float>();
    if (!(readStockFile())) {
	System.exit(1);
    }
	
    serverSocket = new DatagramSocket(stockPort);
    register();
    System.out.println("Store test");
	
    byte[] receiveData = new byte[1024];
    byte[] sendData = new byte[1024];
	
    while (true) {

	DatagramPacket recvPacket = new DatagramPacket(receiveData, receiveData.length);
	    
	serverSocket.receive(recvPacket);
	String request = new String(recvPacket.getData());
	System.out.println(request);
	    
	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, recvPacket.getAddress(), recvPacket.getPort());

	// send ACK back to sender
	String ack = "ACK";
	sendData = ack.getBytes();
	sendPacket.setData(sendData);
	serverSocket.send(sendPacket);

	String response = parseMessage(request.trim());
	sendData = null;
	sendData = new byte[1024];

	sendData = response.getBytes();
	sendPacket.setData(sendData);

	waitForAck(serverSocket, sendPacket, recvPacket);
	    
	receiveData = null;
	receiveData = new byte[1024];
	sendData = null;
	sendData = new byte[1024];
    }
	
}
    
public static void main(String args[]) throws Exception {
    // Ensure the command line arguments are correct
    if (args.length != 3) {
	System.err.println("Invalid command line arguments for Store");
	System.exit(1);
    }

    // parse comand line arguments into globals
    stockPort = Integer.parseInt(args[0]);
    stockFile = args[1];
    nameServerPort = Integer.parseInt(args[2]);

    // start up the store server
    new Store();
}
}
