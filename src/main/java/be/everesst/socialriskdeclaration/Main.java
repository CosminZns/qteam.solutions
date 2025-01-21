package be.everesst.socialriskdeclaration;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;

public class Main {
    public static void main(String[] args) {

        S3Client s3Client = S3Client.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        String bucketName = "testBucket";
        S3Service s3Service = new S3Service(bucketName, s3Client);

        Resource parent = new Resource("parent-folder", "parent-folder", 1);
        ListResult<Resource> listResult = s3Service.listFolder(parent, null);
        System.out.println("List Folder Result: " + listResult);

        Resource resource = s3Service.getResource("file1.txt");
        System.out.println("Get Resource Result: " + resource);

        File file = s3Service.getAsFile(resource);
        System.out.println("Get As File Result: " + file.getPath());
    }
}