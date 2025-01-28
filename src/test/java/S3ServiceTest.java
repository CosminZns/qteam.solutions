import be.everesst.socialriskdeclaration.ListResult;
import be.everesst.socialriskdeclaration.Resource;
import be.everesst.socialriskdeclaration.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.util.List;

import static be.everesst.socialriskdeclaration.Type.FILE;
import static be.everesst.socialriskdeclaration.Type.FOLDER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {

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
    public void testListFolder_Success() {
        ListObjectsV2Response mockResponse = ListObjectsV2Response.builder()
                .contents(List.of(
                        S3Object.builder().key("folder1/file1.txt").size(100L).build(),
                        S3Object.builder().key("folder1/file2.txt").size(200L).build()
                ))
                .commonPrefixes(CommonPrefix.builder().prefix("folder1/subfolder1/").build())
                .nextContinuationToken("next-cursor")
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(mockResponse);

        Resource parent = new Resource("folder1/", "folder1", FOLDER.getValue());
        ListResult<Resource> result = s3Service.listFolder(parent, null);

        assertNotNull(result);
        assertEquals(3, result.resources().size());
        assertEquals("next-cursor", result.cursor());

        Resource folder = result.resources().getFirst();
        assertEquals("folder1/subfolder1/", folder.id());
        assertEquals("subfolder1", folder.name());
        assertEquals(FOLDER.getValue(), folder.type());

        Resource file = result.resources().get(1);
        assertEquals("folder1/file1.txt", file.id());
        assertEquals("file1.txt", file.name());
        assertEquals(FILE.getValue(), file.type());
    }

    @Test
    public void testGetResource_Success() {
        HeadObjectResponse mockResponse = HeadObjectResponse.builder()
                .contentLength(1234L)
                .build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(mockResponse);

        Resource result = s3Service.getResource("folder1/file1.txt");

        assertNotNull(result);
        assertEquals("folder1/file1.txt", result.id());
        assertEquals("file1.txt", result.name());
        assertEquals(FILE.getValue(), result.type());
    }

    @Test
    public void testGetAsFile_Success() {
        GetObjectResponse mockResponse = GetObjectResponse.builder().build();
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class))).thenReturn(mockResponse);

        Resource resource = new Resource("folder1/file1.txt", "file1.txt", FILE.getValue());

        File result = s3Service.getAsFile(resource);

        assertNotNull(result);
        assertEquals("file1.txt", result.getName());
    }

    @Test
    public void testListFolder_InvalidBucketName() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("The specified bucket does not exist").build());

        Resource parent = new Resource("invalid-folder", "invalid-folder", FOLDER.getValue());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> s3Service.listFolder(parent, null));
        assertEquals("Failed to list folder contents: The specified bucket does not exist", exception.getMessage());
    }

    @Test
    public void testGetResource_NonExistentFile() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder().errorMessage("The specified key does not exist").build();
        AwsServiceException s3Exception = S3Exception.builder().awsErrorDetails(errorDetails).build();

        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Exception);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> s3Service.getResource("non-existent-file.txt"));
        assertEquals("Error getting resource: The specified key does not exist", exception.getMessage());
    }

    @Test
    public void testGetAsFile_InvalidFilePath() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder().errorMessage("The specified key does not exist").build();
        AwsServiceException s3Exception = S3Exception.builder().awsErrorDetails(errorDetails).build();

        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class))).thenThrow(s3Exception);

        Resource resource = new Resource("invalid-path/file.txt", "file.txt", FILE.getValue());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> s3Service.getAsFile(resource));
        assertEquals("Error getting resource: The specified key does not exist", exception.getMessage());
    }


}
