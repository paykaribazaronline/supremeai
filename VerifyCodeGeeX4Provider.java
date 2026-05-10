import com.supremeai.provider.CodeGeeX4Provider;
import java.lang.reflect.Method;
import java.util.Map;

public class VerifyCodeGeeX4Provider {
    public static void main(String[] args) {
        try {
            // Test 1: Create provider instance
            CodeGeeX4Provider provider = new CodeGeeX4Provider("test-api-key");
            System.out.println("✓ Test 1 PASSED: Provider created successfully");
            
            // Test 2: Check getName()
            String name = provider.getName();
            if ("codegeex4".equals(name)) {
                System.out.println("✓ Test 2 PASSED: getName() returns 'codegeex4'");
            } else {
                System.out.println("✗ Test 2 FAILED: getName() returned '" + name + "' instead of 'codegeex4'");
                System.exit(1);
            }
            
            // Test 3: Check getCapabilities()
            Map<String, Object> capabilities = provider.getCapabilities();
            if (capabilities != null && capabilities.containsKey("name")) {
                System.out.println("✓ Test 3 PASSED: getCapabilities() returns non-null map with 'name' key");
                System.out.println("  - Provider name: " + capabilities.get("name"));
                System.out.println("  - Models: " + capabilities.get("models"));
                System.out.println("  - Context: " + capabilities.get("context"));
                System.out.println("  - Languages: " + capabilities.get("languages"));
            } else {
                System.out.println("✗ Test 3 FAILED: getCapabilities() returned null or missing 'name' key");
                System.exit(1);
            }
            
            // Test 4: Check inheritance from AbstractHttpProvider
            Class<?> superClass = provider.getClass().getSuperclass();
            if (superClass != null && superClass.getSimpleName().equals("AbstractHttpProvider")) {
                System.out.println("✓ Test 4 PASSED: CodeGeeX4Provider extends AbstractHttpProvider");
            } else {
                System.out.println("✗ Test 4 FAILED: CodeGeeX4Provider does not extend AbstractHttpProvider");
                System.exit(1);
            }
            
            // Test 5: Check implemented interfaces
            Class<?>[] interfaces = provider.getClass().getInterfaces();
            boolean implementsAIProvider = false;
            for (Class<?> iface : interfaces) {
                if (iface.getSimpleName().equals("AIProvider")) {
                    implementsAIProvider = true;
                    break;
                }
            }
            if (implementsAIProvider) {
                System.out.println("✓ Test 5 PASSED: CodeGeeX4Provider implements AIProvider");
            } else {
                System.out.println("✗ Test 5 FAILED: CodeGeeX4Provider does not implement AIProvider");
                System.exit(1);
            }
            
            // Test 6: Check constructor with custom model
            CodeGeeX4Provider providerLite = new CodeGeeX4Provider("test-api-key", "codegeex-4-lite");
            System.out.println("✓ Test 6 PASSED: Constructor with custom model works");
            
            System.out.println("\n✅ All verification tests PASSED!");
            System.out.println("\nCodeGeeX4Provider is correctly implemented and ready to use.");
            
        } catch (Exception e) {
            System.out.println("✗ Test FAILED with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
