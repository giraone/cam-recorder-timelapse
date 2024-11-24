package com.giraone.streaming.service.model;

import java.util.Comparator;

public record FileInfoOrder(String attribute, boolean desc) {

    private static final Comparator<? super FileInfo> comparatorFileName = Comparator.comparing(FileInfo::fileName);
    private static final Comparator<? super FileInfo> comparatorLastModified = Comparator.comparing(FileInfo::lastModified);
    private static final Comparator<? super FileInfo> comparatorInfos = Comparator.comparing(FileInfo::infos);
    private static final Comparator<? super FileInfo> comparatorSizeInBytes = Comparator.comparing(FileInfo::sizeInBytes);

    public Comparator<? super FileInfo> getComparator() {
        Comparator<? super FileInfo> ret;
        if ("fileName".equals(attribute)) {
            ret = comparatorFileName;
        } else if ("lastModified".equals(attribute)) {
            ret =  comparatorLastModified;
        } else if ("infos".equals(attribute)) {
            ret =  comparatorInfos;
        } else if ("sizeInBytes".equals(attribute)) {
            ret =  comparatorSizeInBytes;
        } else {
            throw new IllegalArgumentException("Illegal sort attribute \"" + attribute + "\"!");
        }
        if (desc) {
            ret = ret.reversed();
        }
        return ret;
    }
}
