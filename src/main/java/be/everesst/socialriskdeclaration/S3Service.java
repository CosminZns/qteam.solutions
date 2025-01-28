package be.everesst.socialriskdeclaration;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static be.everesst.socialriskdeclaration.Type.FILE;
import static be.everesst.socialriskdeclaration.Type.FOLDER;

public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(String bucketName, S3Client s3Client) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public ListResult<Resource> listFolder(Resource parent, String cursor) {
        try {
            String prefix = parent != null ? parent.name() + "/" : "";
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .delimiter("/")
                    .continuationToken(cursor);

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            List<Resource> resources = new ArrayList<>();

            response.commonPrefixes().forEach(prefixPath -> addFolders(prefixPath, prefix, resources));
            response.contents().forEach(content -> addFiles(content, prefix, resources));

            return new ListResult<>(resources, response.nextContinuationToken());
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to list folder contents: " + e.getMessage(), e);
        }

    }

    public Resource getResource(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be null or empty");
        }
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(id)
                    .build();
            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

            String name = id.substring(id.lastIndexOf('/') + 1);
            int type = headObjectResponse.contentLength() == 0 ? FOLDER.getValue() : FILE.getValue();

            return new Resource(id, name, type);
        } catch (S3Exception e) {
            throw new RuntimeException("Error getting resource: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public File getAsFile(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource cannot be null");
        }
        try {
            File file = new File(resource.name());
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(resource.id())
                    .build();

            s3Client.getObject(request, ResponseTransformer.toFile(Paths.get(file.getAbsolutePath())));

            return file;
        } catch (S3Exception e) {
            throw new RuntimeException("Error getting resource: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private void addFiles(S3Object content, String prefix, List<Resource> resources) {
        if (!content.key().equals(prefix)) {
            String name = content.key().substring(prefix.length());
            resources.add(new Resource(content.key(), name, FILE.getValue()));
        }
    }

    private void addFolders(CommonPrefix prefixPath, String prefix, List<Resource> resources) {
        String name = prefixPath.prefix().substring(prefix.length(), prefixPath.prefix().length() - 1);
        resources.add(new Resource(prefixPath.prefix(), name, FOLDER.getValue()));
    }

}