package com.fileserver.app.entity.file;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public @Data class Resolution {
    private Integer width;
    private Integer height;
    private String label;

    public static List<Resolution> getImageFormatList() {
        List<Resolution> list = new ArrayList<>();
        Resolution first = new Resolution(180, 180, "180x180");
        Resolution second = new Resolution(90, 90, "90x90");
        Resolution third = new Resolution(1200, 630, "1200x630");
        list.add(first);
        list.add(second);
        list.add(third);
        return list;
    }
}
