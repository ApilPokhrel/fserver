package com.fileserver.app.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.fileserver.app.entity.file.ImageDetail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ImageService {

    @Value("${app.upload.dir}")
    public String uploadDir;

    public ImageDetail detail(String name) throws IOException {
        BufferedImage image = ImageIO.read(new File(getFullPath(name)));
        ImageDetail dt = new ImageDetail();
        dt.setWidth(image.getWidth());
        dt.setHeight(image.getHeight());
        return dt;
    }

    public void resize(String name, String type, Integer width, Integer height, String newName) throws IOException {
        BufferedImage image = ImageIO.read(new File(getFullPath(name)));
        BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
        ImageIO.write(resizedImage, type, new File(getFullPath(newName))); // write the image to a file
    }

    private String getFullPath(String name) {
        return uploadDir + File.separator + StringUtils.cleanPath(name);
    }

}
