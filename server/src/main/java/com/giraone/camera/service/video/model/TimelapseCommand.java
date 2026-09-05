package com.giraone.camera.service.video.model;

import java.util.List;

/**
 *
 * @param outputFilename Output file name
 * @param inputFileNames List of input file names
 * @param select Modulo select of images. 1 = Use every image, 2 = use every second image.
 * @param frameRate Frame rate (5-60)
 */
public record TimelapseCommand(String outputFilename,
                               List<String> inputFileNames,
                               int select,
                               int frameRate) {
}
