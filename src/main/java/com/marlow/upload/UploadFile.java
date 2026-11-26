package com.marlow.upload;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** The uploaded CSV, kept in the database so every running copy of the app can read it. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "upload_files")
public class UploadFile {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(nullable = false, length = 20 * 1024 * 1024)
    private byte[] content;
}
