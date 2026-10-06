package com.example.filestreaming.domain.storage;


import com.example.filestreaming.domain.dto.StreamContentDto;
import com.example.filestreaming.domain.enumerator.AttachmentType;
import org.springframework.lang.Nullable;

public interface ObjectStorage {

    StreamContentDto streamContent(AttachmentType attachmentType, @Nullable String range);

}
