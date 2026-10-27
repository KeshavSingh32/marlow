package com.marlow.upload;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploads;

    public UploadController(UploadService uploads) {
        this.uploads = uploads;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UploadResponse create(@RequestPart("file") MultipartFile file) {
        return uploads.accept(file);
    }

    @GetMapping
    public List<UploadResponse> list() {
        return uploads.list();
    }

    @GetMapping("/{id}")
    public UploadResponse get(@PathVariable UUID id) {
        return uploads.get(id);
    }
}
