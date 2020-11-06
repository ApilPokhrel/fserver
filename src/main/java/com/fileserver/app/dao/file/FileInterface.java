package com.fileserver.app.dao.file;

import java.util.List;
import java.util.Optional;

import com.fileserver.app.entity.file.File;

import org.springframework.stereotype.Repository;

@Repository
public interface FileInterface {
    Optional<File> add(File file);
    Optional<File> getById(String id);
    Optional<File> getByName(String name);
    Optional<File> updateByName(String name, String key, Object value);
    List<File> incompletes(boolean isParent);
    Optional<File> incompleted(String parent);
}
