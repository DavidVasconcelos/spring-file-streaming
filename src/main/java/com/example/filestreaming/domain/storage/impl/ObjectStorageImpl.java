package com.example.filestreaming.domain.storage.impl;

import com.example.filestreaming.domain.dto.StreamContentDto;
import com.example.filestreaming.domain.enumerator.AttachmentType;
import com.example.filestreaming.domain.storage.ObjectStorage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Service
public class ObjectStorageImpl implements ObjectStorage {

  private final String IMAGE_FILE_NAME = "artemis.jpg";
  private final String VIDEO_FILE_NAME = "artemis.mp4";

  @Override
  public StreamContentDto streamContent(AttachmentType attachmentType, String range) {
    Assert.notNull(attachmentType, "Attachment type must not be null");
    log.info("Streaming content for type: {}, requested range: {}", attachmentType, range);

    try {
      Path filePath = getObjectPath(attachmentType);
      long fileSize = Files.size(filePath);
      Pair<Long, Long> ranges = parseRange(range, fileSize);

      return new StreamContentDto(
          range != null,
          getMediaType(attachmentType),
          calculateContentLength(ranges),
          formatContentRange(ranges, fileSize),
          createStreamingResponseBody(filePath, ranges)
      );
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Media file not found for: " + attachmentType, e);
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while preparing stream", e);
    }
  }

  private Path getObjectPath(AttachmentType attachmentType) throws Exception {
    switch (attachmentType) {
      case VIDEO -> {
        return new ClassPathResource(VIDEO_FILE_NAME).getFile().toPath();
      }
      case IMAGE -> {
        return new ClassPathResource(IMAGE_FILE_NAME).getFile().toPath();
      }
      case FILE -> {
        return createMediaZip();
      }
      default -> throw new IllegalArgumentException("Unknown type: " + attachmentType);
    }
  }

  private Path createMediaZip() throws IOException {
    Path zipPath = Files.createTempFile("download", ".zip");
    log.info("Generating temp ZIP file at: {}", zipPath.toAbsolutePath());

    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
      addResourceToZip(zos, VIDEO_FILE_NAME);
      addResourceToZip(zos, IMAGE_FILE_NAME);
    }

    return zipPath;
  }

  private void addResourceToZip(ZipOutputStream zos, String fileName) throws IOException {
    ClassPathResource resource = new ClassPathResource(fileName);
    if (!resource.exists()) {
      log.warn("Could not find {} to add to ZIP", fileName);
      return;
    }

    zos.putNextEntry(new ZipEntry(fileName));

    try (InputStream is = resource.getInputStream()) {
      is.transferTo(zos);
    }

    zos.closeEntry();
  }

  private Pair<Long, Long> parseRange(String range, long fileSize) {
    if (range == null || !range.startsWith("bytes=")) {
      return Pair.of(0L, fileSize - 1);
    }

    try {
      String[] bounds = range.substring(6).split("-");
      long start = Long.parseLong(bounds[0]);

      long end = (bounds.length > 1 && !bounds[1].isEmpty())
          ? Long.parseLong(bounds[1])
          : fileSize - 1;

      end = Math.min(end, fileSize - 1);

      return Pair.of(start, end);
    } catch (NumberFormatException | IndexOutOfBoundsException e) {
      log.warn("Malformed Range header '{}'. Falling back to full file.", range);
      return Pair.of(0L, fileSize - 1);
    }
  }

  private String getMediaType(AttachmentType attachmentType) {
    return switch (attachmentType) {
      case VIDEO -> "video/mp4";
      case IMAGE -> "image/jpg";
      case FILE -> "application/zip";
    };
  }

  private static long calculateContentLength(Pair<Long, Long> ranges) {
    return (ranges.getRight() - ranges.getLeft()) + 1;
  }

  private static String formatContentRange(Pair<Long, Long> ranges, long fileSize) {
    return "bytes %d-%d/%d".formatted(ranges.getLeft(), ranges.getRight(), fileSize);
  }

  private static StreamingResponseBody createStreamingResponseBody(Path filePath,
      Pair<Long, Long> ranges) {
    log.info("Controller Thread: {}", Thread.currentThread().getName());
    return os -> {
      try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
        log.info("Streaming Thread: {}", Thread.currentThread().getName());
        long pos = ranges.getLeft();
        file.seek(pos);

        long bytesToRead = calculateContentLength(ranges);
        byte[] buffer = new byte[8192];

        while (bytesToRead > 0) {
          int readLen = (int) Math.min(buffer.length, bytesToRead);
          int bytesRead = file.read(buffer, 0, readLen);

          if (bytesRead == -1) {
            break;
          }

          os.write(buffer, 0, bytesRead);
          bytesToRead -= bytesRead;
        }
        os.flush();

      } catch (Exception ex) {
        if (ex.getClass().getSimpleName().contains("ClientAbortException") ||
            (ex.getMessage() != null && ex.getMessage().contains("Broken pipe"))) {
          log.debug("Client disconnected before stream completed (Broken Pipe).");
        } else {
          log.error("Streaming interrupted: {}", ex.getMessage());
        }
      }
    };
  }
}