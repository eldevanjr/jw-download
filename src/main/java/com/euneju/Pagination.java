package com.euneju;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pagination
{
    private int totalCount;

    private int offset;

    private int limit;

}