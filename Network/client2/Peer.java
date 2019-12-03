import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
public class Peer {
    
    private class ThreadToListen extends Thread{
        final int uploadPort;
        ServerSocket uploadServerSocket;   
        Socket uploadSocket;
        ObjectOutputStream uploadOut;
        ObjectInputStream uploadIn;
        ThreadToListen(int port){
            this.uploadPort = port;
        }
        public void run(){
            try {
                uploadServerSocket = new ServerSocket(uploadPort);
                System.out.println("Waiting for upload neighbour...");
                uploadSocket = uploadServerSocket.accept();
                System.out.println("Connected by upload neighbour...");
                uploadOut = new ObjectOutputStream(uploadSocket.getOutputStream());
                uploadIn = new ObjectInputStream(uploadSocket.getInputStream());
                boolean dc = false;
                while(!dc){

                    String message = (String) uploadIn.readObject();
                    
                    
                    if(message.equals("Request Chunk Set")){
                        uploadOut.writeObject(new ArrayList<>(chunkMap.keySet()));
                    }else if(message.equals("Done")){
                        System.out.println("Receive message: " + message);
                        dc = true;
                    }else{
                        System.out.println("Receive message: " + message);
                        String[] parts = message.split(" ");
                        String chunkName = String.format("%s.%03d", "test.pdf", Integer.parseInt(parts[2]));
                        File file = new File("./chunks/" + chunkName);

                        if(file.isFile()){
                            uploadOut.writeObject("Found");
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
                                uploadOut.write(contents);
                            }
                            contents = new byte[]{'e','n','d'};
                            uploadOut.write(contents);
                            uploadOut.flush();
                        }else{
                            uploadOut.writeObject("NotFound");
                        }
                    }
                }
            } catch (ClassNotFoundException ex) {
                System.out.println(ex);
            } catch (IOException ex) {
                System.out.println(ex);
            } finally{
                try {
                    
                    uploadOut.close();
                    uploadIn.close();
                    System.out.println("Disconnected from upload neighbour");
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }
        }
    }
    
    int numOfChunks;
    
    final String server;
    final int serverPort;
    Socket serverSocket;   
    ObjectOutputStream serverOut;
    ObjectInputStream serverIn;
    
    final String download;
    final int downloadPort;
    Socket downloadSocket;   
    ObjectOutputStream downloadOut;
    ObjectInputStream downloadIn;
    
    ThreadToListen threadToListen;
    
    Map<Integer, Integer> chunkMap;
    
    public Peer(int serverPort, int uploadPort, int downloadPort) throws IOException, InterruptedException, ClassNotFoundException{
        this.server = "localhost";
        this.serverPort = serverPort;
        
        this.download = "localhost";
        this.downloadPort = downloadPort;
        
        numOfChunks = 0;
        
        chunkMap = new ConcurrentHashMap<>();
        
        connectToServer();
        
        threadToListen = new ThreadToListen(uploadPort);
        threadToListen.start();
        
        connectToDownload();
        
        getChunks();
        
        mergechunks(create_List_Of_Files(new File("./chunks/test.pdf.000")),new File("./test.pdf"));
        
        threadToListen.join();
    }
    
    private void getChunks() throws IOException, ClassNotFoundException, InterruptedException{
        while(chunkMap.size() < numOfChunks){
            getChunkFromServer();
            getChunkFromDownload();
            getChunkFromDownload();

        }
        closeServer();
        closeDownload();
    }
    
    private void closeServer() throws IOException{
        serverOut.writeObject("Done");
        serverOut.close();
        serverIn.close();
        System.out.println("Disconnected from server");
    }
    private void closeDownload() throws IOException{
        downloadOut.writeObject("Done");
        downloadOut.close();
        downloadIn.close();
        System.out.println("Disconnected from download neighbour");
    }
    
    private void getChunkFromServer() throws IOException, ClassNotFoundException{
        int missingChunkId = checkMissingChunkFromServer();
        
        if(missingChunkId == -1) return;
        serverOut.writeObject("Request Chunk " + missingChunkId);
        
        if(((String)serverIn.readObject()).equals("Found")){
            byte[] contents = new byte[10000];
            
            String chunkName = String.format("%s.%03d", "test.pdf", missingChunkId);
            
            FileOutputStream fos = new FileOutputStream("./chunks/" + chunkName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            int bytesRead = 0;

            while((bytesRead = serverIn.read(contents)) != -1){
                if(bytesRead >= 3 && (char)contents[bytesRead-3] == 'e' && (char)contents[bytesRead-2] == 'n' && (char)contents[bytesRead - 1] == 'd') {
                    bos.write(contents, 0, bytesRead-3);
                    break;
                }else{
                    bos.write(contents, 0, bytesRead);
                }
            }
            bos.flush();
            System.out.println("Get chunk " + missingChunkId + " from server");
            chunkMap.put(missingChunkId, missingChunkId);
        }else{
            System.out.println("File Not Found");
        }
    }
    
    private void getChunkFromDownload() throws IOException, ClassNotFoundException, InterruptedException{
        int count = 0;
        int missingChunkId = -1;
        while(true){
            downloadOut.writeObject("Request Chunk Set");
        
            ArrayList<Integer> downloadChunkList = (ArrayList<Integer>) downloadIn.readObject();
            missingChunkId = checkMissingChunkFromDownload(downloadChunkList);
            if(missingChunkId == -1) {
                if(count == 1) return;
                count++;
                Thread.sleep(1000);
                continue;
            }
            downloadOut.writeObject("Request Chunk " + missingChunkId);
            break;
        }
        
        
        if(((String)downloadIn.readObject()).equals("Found")){
            byte[] contents = new byte[10000];
            
            String chunkName = String.format("%s.%03d", "test.pdf", missingChunkId);
            
            FileOutputStream fos = new FileOutputStream("./chunks/" + chunkName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            int bytesRead = 0;

            while((bytesRead = downloadIn.read(contents)) != -1){
                if(bytesRead >= 3 && (char)contents[bytesRead-3] == 'e' && (char)contents[bytesRead-2] == 'n' && (char)contents[bytesRead - 1] == 'd') {
                    bos.write(contents, 0, bytesRead-3);
                    break;
                }else{
                    bos.write(contents, 0, bytesRead);
                }
            }
            bos.flush();
            System.out.println("Get chunk " + missingChunkId + " from download neighbour");
            chunkMap.put(missingChunkId, missingChunkId);
        }else{
            System.out.println("File Not Found");
        }
    }
    
    private int checkMissingChunkFromServer(){
        
        List<Integer> missedChunk = new ArrayList<>();
        for(int i = 0; i < numOfChunks; i++){
            if(!chunkMap.containsKey(i)) missedChunk.add(i);
        }
        if(missedChunk.isEmpty()) return -1;
        
        Random random = new Random();
        return(missedChunk.get(random.nextInt(missedChunk.size())));
    }
    
    private int checkMissingChunkFromDownload(ArrayList<Integer> downloadChunkList){
        
        List<Integer> missedChunk = new ArrayList<>();
        for(Integer chunk: downloadChunkList){
            if(!chunkMap.containsKey(chunk)) missedChunk.add(chunk);
        }
        
        if(missedChunk.size() == 0) return -1;
        
        Random random = new Random();
        return(missedChunk.get(random.nextInt(missedChunk.size())));
    }
    
    private void connectToServer() throws IOException, InterruptedException {
        while(true){
            try{
                serverSocket = new Socket(server, serverPort);
                System.out.println("Connected to server...");
                serverOut = new ObjectOutputStream(serverSocket.getOutputStream());
                serverIn = new ObjectInputStream(serverSocket.getInputStream());
                numOfChunks = (Integer) serverIn.readObject();
                System.out.println("Total number of chunks " + numOfChunks + "...");
                break;
            }catch (Exception e){
                System.out.println("Trying to reconnect server...");
                Thread.sleep(5000);
            }
        }
            
    }
    
    private void connectToDownload() throws IOException, InterruptedException{
        while(true){
            try{
                downloadSocket = new Socket(download, downloadPort);
                System.out.println("Connected to download neighbour...");
                downloadOut = new ObjectOutputStream(downloadSocket.getOutputStream());
                downloadIn = new ObjectInputStream(downloadSocket.getInputStream());
                break;
            }catch (Exception e){
                System.out.println("Trying to reconnect download neighbour...");
                Thread.sleep(5000);
            }
        }
        
    }
    
    public static void mergechunks(List<File> files, File output) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(output);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            for (File f : files) {
                Files.copy(f.toPath(), bos);
            }
        }
    }

    public static List<File> create_List_Of_Files(File start) {
        String filename = start.getName();
        String originFileName = filename.substring(0, filename.lastIndexOf("."));
        File[] files = start.getParentFile().listFiles(
                (File dir, String name) -> name.matches(originFileName + "[.]\\d+"));
        Arrays.sort(files);//ensuring order 001, 002, ..., 010, ...
        return Arrays.asList(files);
    }

    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException{
        int serverP = Integer.parseInt(args[0]);
        int uploadP = Integer.parseInt(args[1]);
        int downloadP = Integer.parseInt(args[2]);
        Peer peer = new Peer(serverP,uploadP,downloadP);
        
        
    }


}
