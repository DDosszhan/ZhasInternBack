package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
    List<StoredFile> findByIdIn(Collection<UUID> ids);
}
