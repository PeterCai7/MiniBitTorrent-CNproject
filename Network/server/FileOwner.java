import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class FileOwner {
    final int sPort;   //The server will be listening on this port number
    ServerSocket sSocket;   //serversocket used to listen on port number 8000
    Map<String, String> account;
    int chunkId = 0;
    AtomicInteger connectedHost;
    
    public static void main(String args[]) throws IOException {
        if(args.length != 1){
            System.out.println("Incorrect arguments. Usage: FileOwner [port_number]");
        }else{
           FileOwner s = new FileOwner(Integer.parseInt(args[0]));
        } 
    }
	
    public FileOwner(int sPort) throws IOException {
        connectedHost = new AtomicInteger(0);
    	this.sPort = sPort;
        breakFile(new File("./test.pdf"));
        startListen();
        
    }
    
    private void breakFile(File f) throws IOException {
        
        int sizeOfChunks = 100 * 1024; //100kb
        byte[] buffer = new byte[sizeOfChunks];
        //System.out.println(f.getParent());
        String filename = f.getName();

        try (FileInputStream input = new FileInputStream(f);
             BufferedInputStream binput = new BufferedInputStream(input)) {

            int bytesAmount = 0;
            while ((bytesAmount = binput.read(buffer)) > 0) {
                String chunkName = String.format("%s.%03d", filename, chunkId++);
                File newFile = new File("./chunks/"+chunkName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
    }

    private void startListen(){
        Socket connection = null;
        
        while(true){
            try {
                if(sSocket == null || sSocket.isClosed()){
                    //create a serversocket
                    sSocket = new ServerSocket(sPort, 10);
                    //Wait for connection
                    System.out.println("Waiting for connection");
                    //accept a connection from the client
                }
                
                connection = sSocket.accept();
                
            } catch (IOException ex) {
                System.out.println(ex);
            }
            
            System.out.println("Connection received from " + connection.getInetAddress().getHostName());

            try {
                ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(connection.getInputStream());
                out.flush();
                Thread t = new ClientHandler(connection, in, out); 
                t.start();
            } catch (IOException ex) {
                System.out.println(ex);

            } 
        }
    }
    
    public class ClientHandler extends Thread{
        final ObjectInputStream in;
        final ObjectOutputStream out; 
        final Socket s;
        
        public ClientHandler(Socket s, ObjectInputStream in, ObjectOutputStream out){
            this.s = s;
            this.in = in;
            this.out = out;
        }
        
        @Override
        public void run(){
            boolean dc = false;
            try{
                connectedHost.incrementAndGet();
                
                while(connectedHost.intValue() != 5){
                    Thread.sleep(1000);
                }
                
                out.writeObject(chunkId);
                while(!dc){

                    String message = (String) in.readObject();
                    
                    System.out.println("Receive message: " + message);
                    if(message.equals("Done")){
                        dc = true;
                    }else{
                        String[] parts = message.split(" ");
                        String chunkName = String.format("%s.%03d", "test.pdf", Integer.parseInt(parts[2]));
                        File file = new File("./chunks/" + chunkName);
                        if(file.isFile()){
                            out.writeObject("Found");
                            FileInputStream fis = new FileInputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            byte[] contents;
                            long fileLength = file.length();
                            long current = 0;
                            while(current != fileLength){
                                int size = 10000;
                                if(fileLength - current >= size)
                                    current += size;
                                else{
                                    size = (int) (fileLength - current);
                                    current = fileLength;
                                }
                                contents = new byte[size];
                                bis.read(contents, 0, size);
                                out.write(contents);
                            }
                            contents = new byte[]{'e','n','d'};
                            out.write(contents);
                            out.flush();
                        }else{
                            out.writeObject("NotFound");
                        }
                    }
                }
            } catch (ClassNotFoundException ex) {
                System.out.println(ex);
            } catch (IOException ex) {
                System.out.println(ex);
            } catch (InterruptedException ex) {
                System.out.println(ex);
            } finally{
                try {
                    out.close();
                    in.close();
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
}
