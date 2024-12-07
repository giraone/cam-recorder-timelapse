package com.giraone.camera.service.model;

import java.util.Comparator;

public record FileInfoOrder(String attribute, boolean desc) {

    private static final Comparator<FileInfo> comparatorFileName = Comparator.comparing(FileInfo::getFileName);
    private static final Comparator<FileInfo> comparatorLastModified = Comparator.comparing(FileInfo::getLastModified);
    private static final Comparator<FileInfo> comparatorInfos = Comparator.comparing(FileInfo::getInfos);
    private static final Comparator<FileInfo> comparatorSizeInBytes = Comparator.comparing(FileInfo::getSizeInBytes);

    public Comparator<FileInfo> getComparator() {
        Comparator<FileInfo> ret;
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
