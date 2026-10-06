package com.example.filestreaming.domain.service;

import com.example.filestreaming.domain.dto.StreamContentDto;
import com.example.filestreaming.domain.enumerator.AttachmentType;

public interface AttachmentService {

  StreamContentDto getStreamContent(AttachmentType attachmentType, String range);

}
