package com.giraone.streaming.service.model.timelapse;

import java.util.List;

public record TimelapseCommand(String outputFilename,
                               List<String> inputFileNames,
                               /** Modulo select of images. 1 = Use every images, 2 = use every second image. */int select,
                               /** Frame rate (10-30) */int frameRate) {
}
