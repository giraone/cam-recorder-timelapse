package com.giraone.streaming.service.model.timelapse;

import java.util.List;

public record TimelapseCommand(String outputFilename, List<String> inputFileNames, int rate) {
}
