import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

public class FirebaseServiceTest {

    private FirebaseService firebaseService;
    private FirebaseDatabase mockDatabase;

    @BeforeEach
    public void setUp() {
        mockDatabase = mock(FirebaseDatabase.class);
        firebaseService = new FirebaseService(mockDatabase);
    }

    @Test
    public void testCreateData() {
        // Given
        String key = "testKey";
        Object value = new Object(); // Replace with appropriate value

        // When
        when(mockDatabase.create(key, value)).thenReturn(true);
        boolean result = firebaseService.createData(key, value);

        // Then
        assertTrue(result);
        verify(mockDatabase).create(key, value);
    }

    @Test
    public void testReadData() {
        // Given
        String key = "testKey";
        Object expectedValue = new Object(); // Replace with appropriate value

        // When
        when(mockDatabase.read(key)).thenReturn(expectedValue);
        Object result = firebaseService.readData(key);

        // Then
        assertEquals(expectedValue, result);
        verify(mockDatabase).read(key);
    }

    @Test
    public void testUpdateData() {
        // Given
        String key = "testKey";
        Object newValue = new Object(); // Replace with appropriate value

        // When
        when(mockDatabase.update(key, newValue)).thenReturn(true);
        boolean result = firebaseService.updateData(key, newValue);

        // Then
        assertTrue(result);
        verify(mockDatabase).update(key, newValue);
    }

    @Test
    public void testDeleteData() {
        // Given
        String key = "testKey";

        // When
        when(mockDatabase.delete(key)).thenReturn(true);
        boolean result = firebaseService.deleteData(key);

        // Then
        assertTrue(result);
        verify(mockDatabase).delete(key);
    }

    @Test
    public void testTransaction() {
        // Given
        String key = "testKey";
        Object value = new Object(); // Replace with appropriate value

        // When
        when(mockDatabase.transaction(key, value)).thenReturn(true);
        boolean result = firebaseService.performTransaction(key, value);

        // Then
        assertTrue(result);
        verify(mockDatabase).transaction(key, value);
    }

    @Test
    public void testErrorHandling() {
        // Given
        String key = "testKey";
        Object value = new Object(); // Replace with appropriate value

        // When
        when(mockDatabase.create(key, value)).thenThrow(new FirebaseException("Error occurred"));

        // Then
        assertThrows(FirebaseException.class, () -> {
            firebaseService.createData(key, value);
        });
        verify(mockDatabase).create(key, value);
    }
}