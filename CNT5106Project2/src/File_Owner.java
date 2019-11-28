import java.io.*;

public class File_Owner {

    public static void breakFile(File f) throws IOException {
        int chunkId = 1;
        int sizeOfChunks = 100 * 1024; //100kb
        byte[] buffer = new byte[sizeOfChunks];
        //System.out.println(f.getParent());
        String filename = f.getName();

        try (FileInputStream input = new FileInputStream(f);
             BufferedInputStream binput = new BufferedInputStream(input)) {

            int bytesAmount = 0;
            while ((bytesAmount = binput.read(buffer)) > 0) {
                String chunkName = String.format("%s.%03d", filename, chunkId++);
                File newFile = new File(f.getParent()+"/chunks", chunkName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
    }
    public static void main(String[] args) throws IOException{
        breakFile(new File("./test.pdf"));
    }
}
