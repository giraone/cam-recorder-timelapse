package com.giraone.camera.service;

import com.giraone.camera.config.ApplicationProperties;
import com.giraone.camera.service.api.Settings;
import com.giraone.camera.service.model.FileInfo;
import com.giraone.camera.service.model.FileInfoQuery;
import com.giraone.camera.service.model.Status;
import com.giraone.camera.service.model.timelapse.TimelapseCommand;
import com.giraone.camera.service.model.timelapse.TimelapseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
public class FileViewService {

    private static final Duration DURATION_WAIT_SINGLE = Duration.ofSeconds(20);
    private static final Duration DURATION_WAIT_LIST = Duration.ofSeconds(60);

    private static final Logger LOGGER = LoggerFactory.getLogger(FileViewService.class);

    public static String DIR_NAME_THUMBS = ".thumbs";
    public static String DIR_NAME_META = ".meta";
    public static File STORAGE_BASE = new File("../STORAGE");
    public static File IMAGES_BASE = new File(STORAGE_BASE, "IMAGES");
    public static File IMAGES_THUMBS = new File(IMAGES_BASE, DIR_NAME_THUMBS);
    public static File IMAGES_META = new File(IMAGES_BASE, DIR_NAME_META);
    public static File VIDEOS_BASE = new File(STORAGE_BASE, "VIDEOS");
    public static File VIDEOS_THUMBS = new File(VIDEOS_BASE, DIR_NAME_THUMBS);
    public static File VIDEOS_META = new File(VIDEOS_BASE, DIR_NAME_META);

    static {
        try {
            IMAGES_BASE = IMAGES_BASE.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", IMAGES_BASE, ioe);
        }
        try {
            VIDEOS_BASE = VIDEOS_BASE.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", VIDEOS_BASE, ioe);
        }
        try {
            IMAGES_THUMBS = IMAGES_THUMBS.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", IMAGES_THUMBS, ioe);
        }
        try {
            VIDEOS_THUMBS = VIDEOS_THUMBS.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", VIDEOS_THUMBS, ioe);
        }
        try {
            IMAGES_META = IMAGES_META.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", IMAGES_META, ioe);
        }
        try {
            VIDEOS_META = VIDEOS_META.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", VIDEOS_META, ioe);
        }
    }

    private final ApplicationProperties applicationProperties;

    public FileViewService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public String getThumbUrl(FileInfo fileInfo) {
        return fileInfo.isVideo()
            ? applicationProperties.getHostUrl() + "/video-thumbs/" + fileInfo.fileName().replace(".mp4", ".jpg")
            : applicationProperties.getHostUrl() + "/image-thumbs/" + fileInfo.fileName();
    }

    public int countImageInfos(FileInfoQuery fileInfoQuery) {
        return countFileInfos("image", fileInfoQuery);
    }

    public int countVideoInfos(FileInfoQuery fileInfoQuery) {
        return countFileInfos("video", fileInfoQuery);
    }

    public int countFileInfos(String type, FileInfoQuery fileInfoQuery) {
        LOGGER.debug("countFileInfos {} {}", type, fileInfoQuery);
        return webClient().get().uri(uriBuilder -> uriBuilder
                .path("/" + type + "-count")
                .queryParam("prefixFilter", fileInfoQuery.prefixFilter())
                .build())
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Integer.class)).block();
    }

    public List<FileInfo> listImageInfos(FileInfoQuery fileInfoQuery) {
        return listFileInfos("image", fileInfoQuery);
    }

    public List<FileInfo> listVideoInfos(FileInfoQuery fileInfoQuery) {
        return listFileInfos("video", fileInfoQuery);
    }

    public List<FileInfo> listFileInfos(String type, FileInfoQuery fileInfoQuery) {
        LOGGER.debug("listFileInfos {} {}", type, fileInfoQuery);
        return waitFor(webClient().get().uri(uriBuilder -> uriBuilder
                .path("/" + type + "-infos")
                .queryParam("prefixFilter", fileInfoQuery.prefixFilter())
                .queryParam("offset", fileInfoQuery.offset())
                .queryParam("limit", fileInfoQuery.limit())
                .queryParam("orderAttribute", fileInfoQuery.order().attribute())
                .queryParam("orderDesc", fileInfoQuery.order().desc())
                .build())
            .exchangeToFlux(clientResponse -> clientResponse.bodyToFlux(FileInfo.class)));
    }

    public Status renameImage(FileInfo fileInfo, String name) {
        LOGGER.debug("renameImage {} {}", fileInfo.fileName(), name);
        return waitFor(webClient().put().uri("/images/{filename}", fileInfo.fileName()).bodyValue(name)
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Status.class)));
    }

    public Status renameVideo(FileInfo fileInfo, String name) {
        LOGGER.debug("renameVideo {} {}", fileInfo.fileName(), name);
        return waitFor(webClient().put().uri("/videos/{filename}", fileInfo.fileName()).bodyValue(name)
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Status.class)));
    }

    public Status deleteImage(FileInfo fileInfo) {
        LOGGER.debug("deleteImage {}", fileInfo.fileName());
        return waitFor(webClient().delete().uri("/images/{filename}", fileInfo.fileName())
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Status.class)));
    }

    public Status deleteVideo(FileInfo fileInfo) {
        LOGGER.debug("deleteVideo {}", fileInfo.fileName());
        return waitFor(webClient().delete().uri("/videos/{filename}", fileInfo.fileName())
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Status.class)));
    }

    public void deleteImages(Set<FileInfo> selectedItems) {
        selectedItems.forEach(this::deleteImage);
    }

    public void deleteVideos(Set<FileInfo> selectedItems) {
        selectedItems.forEach(this::deleteVideo);
    }

    public Mono<TimelapseResult> makeTimelapseVideo(TimelapseCommand timelapseCommand) {
        return webClient().post().uri("/video/create-timelapse")
            .body(BodyInserters.fromValue(timelapseCommand))
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(TimelapseResult.class));
    }

    public Mono<String> downloadSelectedImages(List<String> imageNames) {
        return webClient().post().uri("/image/download-as-zip")
            .body(BodyInserters.fromValue(imageNames))
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class));
    }

    //------------------------------------------------------------------------------------------------------------------

    public Mono<Settings> loadSettings() {
        return webClient().get().uri("/settings")
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Settings.class))
            .doOnNext(settings -> LOGGER.info("Load settings {}", settings));
    }

    public Mono<Status> storeSettings(Settings settings) {
        LOGGER.info("Store settings {}", settings);
        return webClient().put().uri("/settings")
            .body(BodyInserters.fromValue(settings))
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Status.class));
    }

    //------------------------------------------------------------------------------------------------------------------

    private WebClient webClient() {
        return WebClient.builder()
            .baseUrl(applicationProperties.getHostUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    private Status waitFor(Mono<Status> statusMono) {
        final Status ret = statusMono.block(DURATION_WAIT_SINGLE);
        LOGGER.debug("waitFor {}", ret);
        return ret != null ? ret : new Status(false, "Timeout (block)!");
    }

    private List<FileInfo> waitFor(Flux<FileInfo> fileInfoFlux) {
        final List<FileInfo> ret = fileInfoFlux.collectList().block(DURATION_WAIT_LIST);
        LOGGER.debug("waitFor {}", ret);
        return ret != null ? ret : List.of();
    }
}
