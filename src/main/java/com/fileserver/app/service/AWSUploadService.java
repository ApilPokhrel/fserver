package com.fileserver.app.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.fileserver.app.config.JaxbForkJoinWorkerThreadFactory;
import com.fileserver.app.exception.AWSUploadException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AWSUploadService {

    @Value("${aws.secret}")
    private String aws_secret;

    @Value("${aws.access}")
    private String aws_access;

    @Value("${app.upload.dir}")
    public String uploadDir;


    public CompletableFuture<Void> multipartUploadAsync(String bucketName, String keyName, String contentType) {
        Regions clientRegion = Regions.US_EAST_2;
        String filePath = uploadDir;

        File file = new File(filePath, keyName);
        long contentLength = file.length();
        long partSize = 10L * 1024 * 1024; // Set part size to 10 MB.

        try {

            AWSCredentials credentials = new BasicAWSCredentials(aws_access, aws_secret);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();

            // Create a list of ETag objects. You retrieve ETags for each object part uploaded,
            // then, after each individual part has been uploaded, pass the list of ETags to
            // the request to complete the upload.
            List<PartETag> partETags = new ArrayList<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
            initRequest.withCannedACL(CannedAccessControlList.PublicRead);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            initRequest.setObjectMetadata(metadata);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                // Because the last part could be less than 5 MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));
                final int index = i;
                final long fP = filePosition;
                final long pS = partSize;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // Create the request to upload a part.
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName(bucketName)
                            .withKey(keyName)
                            .withUploadId(initResponse.getUploadId())
                            .withPartNumber(index)
                            .withFileOffset(fP)
                            .withFile(file)
                            .withPartSize(pS);

                    // Upload the part and add the response's ETag to our list.
                    UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                    partETags.add(uploadResult.getPartETag());
                });

                futures.add(future);
                filePosition += partSize;
            }

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));

             return allFutures.thenApply(future ->
                        futures.stream()
                        .map(CompletableFuture<Void>::join)
                        .collect(Collectors.toList())
            ).thenAcceptAsync(l -> {
                    // Complete the multipart upload.
                    CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, keyName,
                    initResponse.getUploadId(), partETags);
                    s3Client.completeMultipartUpload(compRequest);
            }, getJaxbExecutor());

        } catch (AmazonServiceException e) {
            e.printStackTrace();
           throw new AWSUploadException(e.getErrorMessage());
        } catch (SdkClientException ex) {
            ex.printStackTrace();
            throw new AWSUploadException(ex.getLocalizedMessage());
        }
    }

    public void multipartUploadSync(String bucketName, String keyName, String contentType) {
        Regions clientRegion = Regions.US_EAST_2;
        String filePath = uploadDir;

        File file = new File(filePath, keyName);
        long contentLength = file.length();
        long partSize = 10 * 1024 * 1024L; // Set part size to 10 MB.

        try {
            AWSCredentials credentials = new BasicAWSCredentials(aws_access, aws_secret);

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(clientRegion)
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();

            List<PartETag> partETags = new ArrayList<>();

            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
            initRequest.withCannedACL(CannedAccessControlList.PublicRead);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            initRequest.setObjectMetadata(metadata);

            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                partSize = Math.min(partSize, (contentLength - filePosition));

                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName)
                        .withKey(keyName)
                        .withUploadId(initResponse.getUploadId())
                        .withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSize);

                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, keyName,
                    initResponse.getUploadId(), partETags);
            s3Client.completeMultipartUpload(compRequest);
        } catch (AmazonServiceException e) {
            throw new AWSUploadException(e.getErrorMessage());
        } catch (SdkClientException ex) {
            throw new AWSUploadException(ex.getLocalizedMessage());
        }
    }


    public void upload(String bucketName, String keyName, String contentType){
        Regions clientRegion = Regions.US_EAST_2;
        String filePath = uploadDir;
        try {

            AWSCredentials credentials = new BasicAWSCredentials(aws_access, aws_secret);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();

                    PutObjectRequest request = new PutObjectRequest(bucketName, keyName, new File(filePath, keyName));
                    request.withCannedAcl(CannedAccessControlList.PublicRead);
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentType(contentType);
                    request.setMetadata(metadata);
                    s3Client.putObject(request);

        } catch (AmazonServiceException e) {
            throw new AWSUploadException(e.getErrorMessage());
        } catch (SdkClientException ex) {
            ex.printStackTrace();
            throw new AWSUploadException(ex.getLocalizedMessage());
        }
    }

    public void remove(String bucketName, String keyName){
        Regions clientRegion = Regions.US_EAST_2;
        try {

            AWSCredentials credentials = new BasicAWSCredentials(aws_access, aws_secret);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();

                    s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));

        } catch (AmazonServiceException e) {
            e.getStatusCode();
        } catch (SdkClientException ex) {
            ex.getMessage();
        }
    }

    private ForkJoinPool getJaxbExecutor() {
        JaxbForkJoinWorkerThreadFactory threadFactory = new JaxbForkJoinWorkerThreadFactory();
        int parallelism = Math.min(0x7fff /* copied from ForkJoinPool.java */, Runtime.getRuntime().availableProcessors());
        return new ForkJoinPool(parallelism, threadFactory, null, false);
    }

}
