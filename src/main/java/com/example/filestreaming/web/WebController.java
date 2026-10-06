package com.example.filestreaming.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/video")
    public String playVideo() {
        return "stream_video.html";
    }

    @GetMapping("/image")
    public String getImage() {
        return "stream_image.html";
    }

    @GetMapping("/file")
    public String getFile() {
        return "stream_file.html";
    }

}
