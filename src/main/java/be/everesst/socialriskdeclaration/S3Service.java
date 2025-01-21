package be.everesst.socialriskdeclaration;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(String bucketName, S3Client s3Client) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public ListResult<Resource> listFolder(Resource parent, String cursor) {
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(parent != null ? parent.id() + "/" : "")
                    .continuationToken(cursor);

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            List<Resource> resources = response.contents().stream()
                    .map(s3Object -> new Resource(s3Object.key(), s3Object.key(), s3Object.size() == 0 ? 1 : 0))
                    .toList();

            return new ListResult<>(resources, response.nextContinuationToken());
        } catch (S3Exception e) {
            throw new RuntimeException("Error listing folder contents: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public Resource getResource(String id) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(id)
                    .build();

            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

            return new Resource(id, id, headObjectResponse.contentLength() == 0 ? 1 : 0);
        } catch (S3Exception e) {
            throw new RuntimeException("Error getting resource: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public File getAsFile(Resource resource) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(resource.id())
                    .build();

            File file = new File("/tmp/" + resource.name());
            s3Client.getObject(getObjectRequest, Paths.get(file.getPath()));

            return file;
        } catch (S3Exception e) {
            throw new RuntimeException("Error downloading file: " + e.awsErrorDetails().errorMessage(), e);
        }
    }
}