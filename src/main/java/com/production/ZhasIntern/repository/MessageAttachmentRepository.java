package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {
    List<MessageAttachment> findByMessageIdIn(Collection<UUID> messageIds);
}
