package com.euneju;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Category
{
    private String key;

    private String type;

    private String name;

    private String description;

    private List<String> tags;

    private Category parentCategory;

    private List<Media> media;

    private List<Subcategories> subcategories;

}