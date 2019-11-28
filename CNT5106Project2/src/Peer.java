import java.io.*;
import java.nio.file.Files;
import java.util.*;
public class Peer {


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

    public static void main(String[] args) throws IOException{
        mergechunks(create_List_Of_Files(new File("chunks/test.pdf.001")),new File("./test1.pdf"));
    }
}
