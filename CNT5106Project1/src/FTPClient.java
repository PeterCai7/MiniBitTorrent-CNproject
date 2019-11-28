import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class FTPClient {
	Socket requestSocket;           //message socket connect to the server
	ObjectOutputStream out;         //stream write to the socket
 	ObjectInputStream in;          //stream read from the socket
	String message;                //message send to the server
	String MESSAGE;                //message received from the server
	InputStream input = null;
	OutputStream output = null;

	public void FTPClient() {}

	void run(String host, int port)
	{
		try{
			//create a socket to connect to the server
			requestSocket = new Socket(host, port);
			System.out.println("Connected to localhost in port 8000");
			//initialize inputStream and outputStream
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			
			//get Input from standard input
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			// log in
			while(true)
			{
                //Receive the instruction from the server
                MESSAGE = (String)in.readObject();
                //show the instruction to the user
                System.out.println(MESSAGE);
                //read username from the standard input
                message = bufferedReader.readLine();
				//Send username to the server
				sendMessage(message);
                //Receive the instruction from the server
                MESSAGE = (String)in.readObject();
                //show the instruction to the user
                System.out.println(MESSAGE);
                //read password from the standard input
                message = bufferedReader.readLine();
                //Send password to the server
                sendMessage(message);
                //Receive the instruction from the server
                MESSAGE = (String)in.readObject();
                //show the instruction to the user
                System.out.println(MESSAGE);
				if (MESSAGE.equals("Awesome, go ahead!")) break;
			}
			// use command to achieve something
			while (true) {
				//read command from the standard input
				message = bufferedReader.readLine();
				if(message.equals("dir")) {
					//Send command to the server
					sendMessage(message);
					//Receive the message from the server
					MESSAGE = (String)in.readObject();
					//show the message to the user
					System.out.println(MESSAGE);
					continue;
				}
				String[] parts_of_command = message.split(" ");
				if (parts_of_command[0].equals("get")) {
					//Send command to the server
					sendMessage(message);
					try {
						input = requestSocket.getInputStream();
					} catch (IOException ex) {
						System.out.println("Can't get socket input stream. ");
					}
					try {
						output = new FileOutputStream(parts_of_command[1]);
					} catch (FileNotFoundException ex) {
						System.out.println("File not found. ");
					}
					byte[] buffer = new byte[16];
					int count;
					while ((count = input.read(buffer)) > 0) {
						output.write(buffer, 0, count);
						if (count < 16) break;
					}
					output.close();
					System.out.println("File received!");
				} else if (parts_of_command[0].equals("upload")) {
					//Send command to the server
					sendMessage(message);
					try {
						File myFile = new File (parts_of_command[1]);
						byte [] buffer  = new byte [32];
						input = new FileInputStream(myFile);
						output = requestSocket.getOutputStream();
						int count;
						while ((count = input.read(buffer)) > 0) {
							output.write(buffer, 0, count);
						}
						input.close();
						System.out.println("Complete Sending");
					}
					catch (IOException ioException) {
						System.out.println("File not found!");
					}
				} else System.out.println("Your command is invalid!");
			}
		}
		catch (ConnectException e) {
    			System.err.println("Connection refused. You need to initiate a server first.");
		} 
		catch ( ClassNotFoundException e ) {
            		System.err.println("Class not found");
        	} 
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//Close connections
			try{
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	//send a message to the output stream
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	//main method
	public static void main(String args[])
	{
		String host = args[0];
		String port = args[1];
		int p = Integer.parseInt(port);
		FTPClient client = new FTPClient();
		client.run(host, p);
	}

}
