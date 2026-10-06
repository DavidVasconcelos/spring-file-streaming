package com.example.filestreaming.web;

import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_RANGE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.example.filestreaming.domain.dto.StreamContentDto;
import com.example.filestreaming.domain.enumerator.AttachmentType;
import com.example.filestreaming.domain.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamController {

  private final AttachmentService attachmentService;

  // Changed PathVariable to String to handle the lowercase "file" from your HTML
  @GetMapping("/{type}")
  public ResponseEntity<StreamingResponseBody> stream(@PathVariable String type,
      @RequestHeader(value = "Range", required = false) String range) {

    AttachmentType attachmentType = AttachmentType.valueOf(type.toUpperCase());

    return asStreamResponse(attachmentService.getStreamContent(attachmentType, range),
        attachmentType);
  }

  private ResponseEntity<StreamingResponseBody> asStreamResponse(
      StreamContentDto streamContent, AttachmentType attachmentType) {
    if (streamContent.partial()) {
      return asPartialStreamResponse(streamContent, attachmentType);
    } else {
      return asFullStreamResponse(streamContent, attachmentType);
    }
  }

  private ResponseEntity<StreamingResponseBody> asPartialStreamResponse(
      StreamContentDto streamingContent, AttachmentType attachmentType) {
    return ResponseEntity
        .status(HttpStatus.PARTIAL_CONTENT)
        .header(CONTENT_TYPE, streamingContent.mediaType())
        .header(CONTENT_LENGTH, Long.toString(streamingContent.contentLength()))
        .header(ACCEPT_RANGES, "bytes")
        .header(CONTENT_RANGE, streamingContent.contentRange())
        .header(CONTENT_DISPOSITION, getDisposition(attachmentType))
        .body(streamingContent.streamingResponseBody());
  }

  private ResponseEntity<StreamingResponseBody> asFullStreamResponse(
      StreamContentDto streamingContent, AttachmentType attachmentType) {
    return ResponseEntity
        .status(HttpStatus.OK)
        .header(CONTENT_TYPE, streamingContent.mediaType())
        .header(CONTENT_LENGTH, Long.toString(streamingContent.contentLength()))
        .header(ACCEPT_RANGES, "bytes")
        .header(CONTENT_DISPOSITION, getDisposition(attachmentType))
        .body(streamingContent.streamingResponseBody());
  }

  private String getDisposition(AttachmentType attachmentType) {
    return switch (attachmentType) {
      case FILE -> "attachment; filename=\"media-bundle.zip\"";
      case VIDEO, IMAGE -> "inline";
    };
  }
}