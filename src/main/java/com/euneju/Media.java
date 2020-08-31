package com.euneju;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Media
{
    private String guid;

    private String languageAgnosticNaturalKey;

    private String naturalKey;

    private String type;

    private String primaryCategory;

    private String title;

    private String description;

    private String firstPublished;

    private double duration;

    private String durationFormattedHHMM;

    private String durationFormattedMinSec;

    private List<String> tags;

    private List<Files> files;

    private List<String> availableLanguages;

}
