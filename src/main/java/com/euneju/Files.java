package com.euneju;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Files
{
    private String progressiveDownloadURL;

    private String flashStreamingURL;

    private String checksum;

    private Long filesize;

    private String modifiedDatetime;

    private double bitRate;

    private double duration;

    private int frameHeight;

    private int frameWidth;

    private String label;

    private double frameRate;

    private String mimetype;

    private boolean subtitled;

    private String nameToSave;

}