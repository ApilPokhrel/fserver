package com.fileserver.app.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.fileserver.app.exception.FileStorageException;
import com.fileserver.app.exception.NotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    @Value("${app.upload.dir}")
    public String uploadDir;

    public String uploadFile(MultipartFile file) {
        try {
            String name = file.getOriginalFilename();
            if(name == null){
                throw new NotFoundException("file not found");
            }
            Path copyLocation = Paths
                .get(uploadDir + File.separator + StringUtils.cleanPath(name));
            Files.copy(file.getInputStream(), copyLocation, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            e.printStackTrace();
            throw new FileStorageException("Could not store file " + file.getOriginalFilename()
                + ". Please try again!");
        }
        return file.getOriginalFilename();
    }

    public File get(String name){
        return new File(uploadDir + File.separator + StringUtils.cleanPath(name));
    }

    public boolean remove(String name){
        try
        {
            Files.deleteIfExists(Paths.get(uploadDir + File.separator + StringUtils.cleanPath(name)));
        }
        catch(IOException e)
        {
            return false;
        }
        return true;
    }

    public boolean exists(String name){
       File file = new File(uploadDir + File.separator + StringUtils.cleanPath(name));
       return file.exists();
    }
}
