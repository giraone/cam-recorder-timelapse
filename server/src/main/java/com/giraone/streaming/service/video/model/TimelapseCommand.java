package com.giraone.streaming.service.video.model;

import java.util.List;

public record TimelapseCommand(String outputFilename, List<String> inputFileNames, int rate) {
}