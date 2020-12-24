package com.fileserver.app.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.fileserver.app.exception.FileNotDownloadedException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileDownloadService {

    @Value("${app.upload.dir}")
    public String uploadDir;

    public synchronized String nioDownloadFile(String urlName, String name) throws MalformedURLException {

        try {
            URL url = new URL(urlName);
            ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
            String downloadedFile = name;
            FileOutputStream fileOutputStream = new FileOutputStream(
                    uploadDir + File.separator + StringUtils.cleanPath(downloadedFile));
            WritableByteChannel writableByteChannel = fileOutputStream.getChannel();
            //
            //
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while (readableByteChannel.read(buffer) != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    writableByteChannel.write(buffer);

                }
                buffer.clear();
            }
            //
            //
            readableByteChannel.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return downloadedFile;
        } catch (MalformedURLException ex) {
            throw new FileNotDownloadedException("File Not Downloaded", ex);
        } catch (IOException e) {
            throw new FileNotDownloadedException("File Not Downloaded", e);
        }

    }

    public synchronized String downloadFile(String urlName, String name) {
        try (BufferedInputStream in = new BufferedInputStream(new URL(urlName).openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(
                        uploadDir + File.separator + StringUtils.cleanPath(name))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (MalformedURLException ex) {
            throw new FileNotDownloadedException("File Not Downloaded", ex);
        } catch (IOException e) {
            throw new FileNotDownloadedException("File Not Downloaded", e);
        }
        return name;
    }
}
