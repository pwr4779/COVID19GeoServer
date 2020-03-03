import java.io.IOException;

public class Application {
    public static void main(String[] args) throws IOException {
        WatchService watcher = new WatchService();
        watcher.init();
    }
}
