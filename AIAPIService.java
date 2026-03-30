// AIAPIService.java

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class AIAPIService {
    private OkHttpClient client;

    public AIAPIService(long connectTimeout, long readTimeout, long writeTimeout) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
    }

    // Other methods for the service...
}