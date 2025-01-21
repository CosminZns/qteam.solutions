import be.everesst.socialriskdeclaration.ListResult;
import be.everesst.socialriskdeclaration.Resource;
import be.everesst.socialriskdeclaration.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3Service2Test {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    public void setUp() {
        String bucketName = "test-bucket";
        s3Service = new S3Service(bucketName, s3Client);
    }

    @Test
    public void testListFolder() {
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("file1.txt").size(100L).build())
                .nextContinuationToken("next-token")
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        Resource parent = new Resource("parent-folder", "parent-folder", 1);
        ListResult<Resource> result = s3Service.listFolder(parent, null);

        assertNotNull(result);
        assertEquals(1, result.resources().size());
        assertEquals("file1.txt", result.resources().get(0).id());
        assertEquals("next-token", result.cursor());
    }

    @Test
    public void testGetResource() {
        HeadObjectResponse response = HeadObjectResponse.builder()
                .contentLength(100L)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);

        Resource resource = s3Service.getResource("file1.txt");

        assertNotNull(resource);
        assertEquals("file1.txt", resource.id());
        assertEquals(0, resource.type());
    }

    @Test
    public void testGetAsFile() {
        GetObjectResponse response = GetObjectResponse.builder().build();

        when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class))).thenReturn(response);

        Resource resource = new Resource("file1.txt", "file1.txt", 0);
        File file = s3Service.getAsFile(resource);

        assertNotNull(file);
        assertEquals("/tmp/file1.txt", file.getPath());
    }

    @Test
    public void testListFolderException() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(S3Exception.builder().message("Error listing folder").build());

        Resource parent = new Resource("parent-folder", "parent-folder", 1);

        assertThrows(RuntimeException.class, () -> s3Service.listFolder(parent, null));
    }

    @Test
    public void testGetResourceException() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder().message("Error getting resource").build());

        assertThrows(RuntimeException.class, () -> s3Service.getResource("file1.txt"));
    }

    @Test
    public void testGetAsFileException() {
        when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class))).thenThrow(S3Exception.builder().message("Error downloading file").build());

        Resource resource = new Resource("file1.txt", "file1.txt", 0);

        assertThrows(RuntimeException.class, () -> s3Service.getAsFile(resource));
    }

}
