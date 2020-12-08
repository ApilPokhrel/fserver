package com.fileserver.app.dao.file;

import java.util.List;
import java.util.Optional;

import com.fileserver.app.entity.file.FileModel;
import com.fileserver.app.entity.file.SubTypeEnum;

import org.springframework.stereotype.Repository;

@Repository
public interface FileInterface {
    Optional<FileModel> add(FileModel file);

    Optional<FileModel> getById(String id);

    Optional<FileModel> getByName(String name);

    Optional<FileModel> updateByName(String name, String key, Object value);

    Optional<FileModel> updateById(String id, String key, Object value);

    Optional<FileModel> updateStatus(String name, boolean uploaded, boolean completed);

    Optional<FileModel> subFile(String parentId, SubTypeEnum subType);

    Optional<FileModel> removeByName(String name);

    Optional<FileModel> removeById(String id);

    Optional<FileModel> getOne(String field, String value);

    Optional<FileModel> removeOne(String field, String value);

    Optional<FileModel> removeChild(String parent, String type);

    List<FileModel> listSubFile(String parentId);
}
