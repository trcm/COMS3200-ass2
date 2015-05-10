/**
 * 
 * @author Mingyang Zhong
 * Feb. 2014
 * the University of Queensland
 * Code example for course: COMS3200
 * 
 This is simple network program based on Java-IO, UDP blocking mode and single thread. 
 The UDPClient reads inputs from the keyboard then sends it to UDPServer.
 The UDPServer reads packets from the socket channel and convert it to upper case, and then sends back to UDPClient. 
 The program assumes that the data in any received packets will be in string form.
 * 
 */

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Arrays;
import java.lang.Thread;

class UDPClient {
    public static Random r;

    public static boolean sendPacket(DatagramPacket packet, DatagramSocket sock, Random r)
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
    
    public static void main(String args[]) throws Exception {
	r = new Random();
	System.out.println(r.nextFloat());
	// construct datagram socket
	DatagramSocket clientSocket = new DatagramSocket();
	// set server's ip address
	InetAddress IPAddress = InetAddress.getByName("127.0.0.1");
	// set buffers
	byte[] sendData = new byte[1024];
	byte[] receiveData = new byte[1024];
	// get the message from user
	System.out.println("Please input your message:");
	BufferedReader inBuf = new BufferedReader(new InputStreamReader(System.in));
	String msg = inBuf.readLine();
	// String msg = "test";
	sendData = msg.getBytes();
	// send the message to server
	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
	// clientSocket.send(sendPacket);
		
	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	clientSocket.setSoTimeout(3000);	
	// check if the data was recieved
	boolean ackd = false;
	
	while (!(ackd)) {
	    sendPacket(sendPacket, clientSocket, r);
	    // simulate packet loss
	    try {
		clientSocket.receive(receivePacket);
		String ack = new String(receivePacket.getData());
		System.out.println("Ack? "+ ack);
		if (ack.trim().equals("ACK")) {
		    System.out.println("acked");
		    ackd = true;
		    break;
		}
	    } catch (SocketTimeoutException e) {
		sendPacket(sendPacket, clientSocket, r);
	    }
	}
	
// receive reply message from server
	System.out.println("waiting for server response");
	clientSocket.receive(receivePacket);
	
	String ack = "ACK";
	Arrays.fill(sendData, (byte)0);
	sendData = ack.getBytes();
	sendPacket.setData(sendData);
	clientSocket.send(sendPacket);
// print the reply
	String upMsg = new String(receivePacket.getData());
	System.out.println("Message from Server:" + upMsg);
// close up
	clientSocket.close();
    }
}
