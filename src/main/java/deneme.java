import java.text.SimpleDateFormat;
import java.util.Date;

public class deneme {
    public static void main(String[] args) {
        SimpleDateFormat f = new SimpleDateFormat("HHmmss");
        System.out.println( f.format(new Date()));
    }
}
