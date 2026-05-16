import com.google.cloud.run.v2.*;
import com.google.iam.v1.*;

public class TestCloudRun {
    public static void main(String[] args) throws Exception {
        ServicesClient client = ServicesClient.create();
        SetIamPolicyRequest req = SetIamPolicyRequest.newBuilder().build();
        Policy p = client.setIamPolicy(req);
    }
}
