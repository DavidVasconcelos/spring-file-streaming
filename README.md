[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=fff)](#)
[![Java](https://img.shields.io/badge/Java-25-%23ED8B00.svg?logo=openjdk&logoColor=white)](#)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-209BC4)](#)

# Spring Boot File Streaming with `StreamingResponseBody`

A study repository demonstrating how to efficiently stream large media files (videos, images) and dynamically bundle file downloads in a Spring Boot application.

This project explores the mechanics of HTTP `Range` requests, dynamic `Content-Disposition` headers, and memory-safe asynchronous streaming using `StreamingResponseBody`.

## Why We Need Data Streaming

Loading entire files into a server's memory before sending them to the client creates severe bottlenecks. By streaming data, we unlock several architectural advantages:

* **Immediate Access to Data:** Users start interacting with the data almost instantly without waiting for the entire dataset to transmit.
* **Reduced Memory and Storage Usage:** Data is processed in tiny chunks (e.g., an 8KB buffer). Only the current portion is loaded into memory, heavily reducing system resource consumption.
* **Better User Experience & Seamless Interaction:** Offers a smoother, responsive experience. Users can interact with the content continuously as new data arrives.
* **Scalability & Large Datasets:** Small, manageable data units allow systems to handle massive volumes of data and large datasets efficiently without overwhelming client or server capacity.
* **Lower Network Bandwidth Usage:** Transmits only the necessary portions of data as needed.
* **Fault Tolerance and Resilience:** Systems can resume data transmission after a failure or a skipped video segment (using HTTP `Range` headers) without restarting from the beginning.

## Key Concepts Demonstrated

### 1. `StreamingResponseBody`

A controller method return value type for asynchronous request processing. It allows the application to write data directly to the HTTP response `OutputStream` without holding up the main Tomcat Servlet container thread.

* **Memory Safety:** By reading files via a `RandomAccessFile` and writing to the `OutputStream` inside a `while` loop, memory usage remains flat (around 8KB per request), even when streaming a 10GB video.
* **Async Processing:** The main HTTP thread is instantly freed to handle other users, while a background thread manages the stream.
*   **Architecture Note (Java 25 & Virtual Threads):** Historically, async streaming required configuring a custom `TaskExecutor` thread pool to prevent OS thread exhaustion. However, because this project uses **Java 25 with Virtual Threads enabled** (`spring.threads.virtual.enabled=true`), Spring Boot automatically assigns a lightweight virtual thread to each stream. The application can handle tens of thousands of concurrent large file downloads with virtually zero thread overhead and no manual pool configuration required.
### 2. HTTP `Range` Requests & Broken Pipes

When users scrub through a video (skipping ahead), the browser terminates the current connection and opens a new one requesting specific byte ranges. This application intercepts the `Range` header, calculates the exact byte offset using `RandomAccessFile.seek()`, and safely catches the resulting `ClientAbortException` / "Broken pipe" errors to prevent log flooding.

### 3. Dynamic `Content-Disposition`

The application intelligently dictates how the browser handles the incoming stream based on the media type:

* **`inline`**: Used for `VIDEO` and `IMAGE`. Tells the browser to render the media directly in the active tab (HTML5 video players, image tags).
* **`attachment`**: Used for `FILE`. Forces the browser to trigger a "Save As" download prompt.

### 4. On-the-Fly ZIP Bundling

Instead of keeping static `.zip` archives, the application demonstrates intercepting a download request to dynamically generate a temporary `.zip` file using `ZipOutputStream`. It bundles multiple resources (like images and videos) together on the hard drive, streams the resulting ZIP back to the user, and maintains low memory overhead.

### 5. Pure Private Helper Methods

Private helper methods (like mathematical byte calculations) are marked as `static` to guarantee state immutability, signal intent, and adhere to Java best practices.

## Endpoints

| Method | Endpoint | Description | Behavior |
| --- | --- | --- | --- |
| `GET` | `/stream/video` | Streams a video file with Range support. | Plays inline in the browser. |
| `GET` | `/stream/image` | Streams a high-resolution image. | Displays inline in the browser. |
| `GET` | `/stream/file` | Bundles the video and image into a ZIP. | Downloads as `media-bundle.zip`. |


