import java.net.URL;
import java.net.HttpURLConnection;

public class test_url {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://supremeai-565236080752.us-central1.run.app/api/status");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            System.out.println("Response Code: " + conn.getResponseCode());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
