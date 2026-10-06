package com.example.filestreaming.domain.service.impl;

import com.example.filestreaming.domain.dto.StreamContentDto;
import com.example.filestreaming.domain.enumerator.AttachmentType;
import com.example.filestreaming.domain.service.AttachmentService;
import com.example.filestreaming.domain.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class AttachmentServiceImpl implements AttachmentService {

  private final ObjectStorage objectStorage;

  @Override
  public StreamContentDto getStreamContent(AttachmentType attachmentType, String range) {
    return objectStorage.streamContent(attachmentType, range);
  }
}
