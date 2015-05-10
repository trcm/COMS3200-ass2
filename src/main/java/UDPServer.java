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

import java.net.*;
import java.lang.Thread;

class UDPServer {
    public static void main(String args[]) throws Exception {
	// construct datagram socket
	DatagramSocket serverSocket = new DatagramSocket(9876);
	// set buffers
	byte[] receiveData = new byte[1024];
	byte[] sendData = new byte[1024];
	System.out.println("<UDPServer> Server is activated.");
	// waiting for incoming messages
	while (true) {
	    // receive message from client
	    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	    serverSocket.receive(receivePacket);
	    // packet recieved, send ACK
	    // get the port of the client
	    InetAddress IPAddress = receivePacket.getAddress();
	    int port = receivePacket.getPort();
	    // Thread.sleep(5000);
	    String ack = "ACK";
	    
	    sendData = ack.getBytes();
	    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
	    serverSocket.send(sendPacket);
	    
	    // print the message
	    String msg = new String(receivePacket.getData());
	    System.out.println("Message from Client: " + msg);
	    // convert the message to upper case
	    String upMsg = msg.toUpperCase();
	    sendData = upMsg.getBytes();
	    sendPacket.setData(sendData);
	    // send the message back to the client 
	    serverSocket.send(sendPacket);
	}
    }
}
