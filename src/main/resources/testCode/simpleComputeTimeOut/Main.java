/**
 * 无限睡眠
 */
public class Main {
    public static void main(String[] args) {
        long Time =10000;

        try {
            Thread.sleep(Time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("睡醒了");
    }
}
