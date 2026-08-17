import java.io.File;
public class Scratch {
    public static void main(String[] args) {
        File parent = null;
        try {
            File f = new File(parent, "foo.bin");
            System.out.println(f.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
