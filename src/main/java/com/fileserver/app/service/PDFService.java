package com.fileserver.app.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.exception.NotSupportedException;
import com.fileserver.app.listener.AdapterListener;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PDFService {

    @Value("${app.upload.dir}")
    public String dir;

    public void generateImages(String parentId, File file, AdapterListener<FileModel> cb) {
        PDDocument document;
        try {
            document = PDDocument.load(file);
        } catch (IOException e) {
            throw new NotSupportedException("cannot read pdf");
        }
        String name = file.getName().replace(".pdf", "_cover");
        PDFRenderer renderer = new PDFRenderer(document);
        String fullDir = dir + File.separator + StringUtils.cleanPath("pdf") + File.separator;
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image;
            try {
                image = renderer.renderImage(i);
            } catch (IOException e) {
                throw new NotSupportedException("cannot read pdf");
            }
            // Writing the image to a file
            String path = fullDir + StringUtils.cleanPath(name + i + ".jpg");
            File f = new File(path);
            try {
                ImageIO.write(image, "JPEG", f);
            } catch (IOException e) {
                throw new NotSupportedException("Cannot create image");
            }
            FileModel fileModel = new FileModel();
            fileModel.setName(name + i);
            fileModel.setPath(fullDir);
            fileModel.setParent_id(new ObjectId(parentId));
            fileModel.set_parent(false);
            fileModel.set_parent(false);
            fileModel.setMimeType("image/jpeg");
            fileModel.setType("image");
            fileModel.setIdn(String.valueOf(i));
            fileModel.setSize(f.length());

            cb.consumer(fileModel);
            // Closing the document
        }
        try {
            document.close();
        } catch (IOException e) {
            throw new NotSupportedException("cannot close pdf");
        }
    }

    public boolean emptyImages() {
        try {
            FileUtils.cleanDirectory(new File(dir + File.separator + StringUtils.cleanPath("pdf")));
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
