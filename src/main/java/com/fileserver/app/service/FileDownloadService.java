package com.fileserver.app.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.fileserver.app.exception.FileNotDownloadedException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileDownloadService {

    @Value("${app.upload.dir}")
    public String uploadDir;

    public synchronized String nioDownloadFile(String urlName, String name) {
        try {
            URL website = new URL(urlName);
            InputStream in = website.openStream();
            Path copyLocation = Paths.get(uploadDir + File.separator + StringUtils.cleanPath(name));
            Files.copy(in, copyLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (MalformedURLException ex) {
            throw new FileNotDownloadedException("File Not Downloaded", ex);
        } catch (IOException e) {
            throw new FileNotDownloadedException("File Not Downloaded", e);
        }

        return name;
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
