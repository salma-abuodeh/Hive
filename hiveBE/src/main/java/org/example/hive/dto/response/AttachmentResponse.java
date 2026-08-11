package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.hive.config.AppEnums.AttachmentType;

@Getter
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private String url;
    private String contentType;
    private Long sizeBytes;
    private AttachmentType attachmentType;
}