import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class FTPServer {

	private static final int sPort = 8000;   //The server will be listening on this port number

	public static void main(String[] args) throws Exception {
		System.out.println("The server is running."); 
        	ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
        	try {
            		while(true) {
            			new Handler(listener.accept(), clientNum).start();
            			System.out.println("Client "  + clientNum + " is connected!");
            			clientNum++;
            		}
        	} finally {
            		listener.close();
        	} 
 
    	}

	/**
     	* A handler thread class.  Handlers are spawned from the listening
     	* loop and are responsible for dealing with a single client's requests.
     	*/
	private static class Handler extends Thread {
		private String username;    // username received from the client
		private String password;    // password received fro the client
		private String message;     // instructions sent to the client
		private String request;     // Request received from the client
		private Socket connection;  // A socket is bind to a specific client by this thread
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int no;		//The index number of the client
		private static Map<String, String> users = new HashMap<>();
		private String current_dir;
		InputStream input = null;
		OutputStream output = null;

		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
			users.put("client1","123");
			users.put("client2","456");
			users.put("client3","789");
		}

		public void run() {
			try{
				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
			    out.flush();
			    in = new ObjectInputStream(connection.getInputStream());
			    current_dir = new File(".").getCanonicalPath();
			    try{
			    	//Let clients pass the username-password examination
			    	while(true) {
			    		// ask the client for username
						message = "Please input your username:";
						//send message to the client
						sendMessage(message);
			    		//receive the username sent from the client
						username = (String)in.readObject();
						//System.out.println(username);
						// ask the client for username
						message = "Please input your password:";
						//send message to the client
						sendMessage(message);
						// receive the password sent from the client
						password = (String)in.readObject();
						if (password.equals(users.get(username))) {
							message = "Awesome, go ahead!";
							sendMessage(message);
							break;
						} else {
							message = "Your username or password is invalid !";
							sendMessage(message);
						}
			    	}
			    	//provide the functionality that FTPServer always serve.
					while(true) {
						request = (String)in.readObject();
						String[] parts_of_request = request.split(" ");
						if (request.equals("dir")) {
							File folder = new File(current_dir);
							File[] list_of_files = folder.listFiles();
							String list_of_filenames = "";
							for (File file: list_of_files) {
								list_of_filenames += "\n" + file.getName();
							}
							sendMessage(list_of_filenames);
						} else if(parts_of_request[0].equals("get")) {
							try {
								File myFile = new File (parts_of_request[1]);
								byte [] buffer  = new byte [16];
								InputStream input = new FileInputStream(myFile);
								OutputStream output = connection.getOutputStream();
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
						} else if(parts_of_request[0].equals("upload")) {
							try {
								input  = connection.getInputStream();
							} catch (IOException ex) {
								System.out.println("Can't get socket input stream. ");
							}
							try {
								output  = new FileOutputStream(parts_of_request[1]);
							} catch (FileNotFoundException ex) {
								System.out.println("File not found. ");
							}
							byte[] buffer = new byte[32];
							int count;
							while ((count = input.read(buffer)) > 0) {
								output.write(buffer, 0, count);
								if (count < 32) break;
							}
							output.close();
							System.out.println("File received!");

						} else sendMessage("Your command is invalid");
					}
			    }
			    catch(ClassNotFoundException classnot){
			    	System.err.println("Data received in unknown format");
			    }
			}
			catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
			}
			finally{
				//Close connections
				try{
					in.close();
					out.close();
					connection.close();
				}
				catch(IOException ioException){
					System.out.println("Disconnect with Client" + no);
				}
			}
		}
		//send a message to the output stream
		public void sendMessage(String msg) {
			try{
				out.writeObject(msg);
				out.flush();
				System.out.println("Send message: " + msg + " to Client " + no);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}

}
