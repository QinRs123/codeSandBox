import java.util.ArrayList;
import java.util.List;

/**
 * 空间异常
 */
public class Main {
    public static void main(String[] args) {
        List<byte[]> list =new ArrayList<>();
        while(true){
            list.add(new byte[10240]);
        }
    }
}
