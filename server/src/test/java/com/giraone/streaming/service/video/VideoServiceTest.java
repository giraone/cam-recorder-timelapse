package com.giraone.streaming.service.video;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.giraone.streaming.service.model.VideoMetaInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoServiceTest {

    @Test
    void buildVideoInfoFromFfmpegJson() throws JsonProcessingException {

        String input = """
            {
                "streams": [
                    {
                        "index": 0,
                        "codec_name": "h264",
                        "codec_long_name": "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
                        "codec_type": "video",
                        "codec_time_base": "1001/48000",
                        "codec_tag_string": "avc1",
                        "codec_tag": "0x31637661",
                        "width": 1280,
                        "height": 720,
                        "has_b_frames": 0,
                        "pix_fmt": "yuv420p",
                        "level": 31,
                        "is_avc": "1",
                        "nal_length_size": "4",
                        "r_frame_rate": "35029/1461",
                        "avg_frame_rate": "35029/1461",
                        "time_base": "1/35029",
                        "start_time": "0.000000",
                        "duration": "1239.195267",
                        "bit_rate": "1782423",
                        "nb_frames": "29711",
                        "tags": {
                            "creation_time": "1970-01-01 00:00:00",
                            "language": "und",
                            "handler_name": "VideoHandler"
                        }
                    },
                    {
                        "index": 1,
                        "codec_name": "aac",
                        "codec_long_name": "Advanced Audio Coding",
                        "codec_type": "audio",
                        "codec_time_base": "1/48000",
                        "codec_tag_string": "mp4a",
                        "codec_tag": "0x6134706d",
                        "sample_fmt": "s16",
                        "sample_rate": "48000",
                        "channels": 2,
                        "bits_per_sample": 0,
                        "r_frame_rate": "0/0",
                        "avg_frame_rate": "0/0",
                        "time_base": "1/48000",
                        "start_time": "0.000000",
                        "duration": "1239.059396",
                        "bit_rate": "127966",
                        "nb_frames": "58081",
                        "tags": {
                            "creation_time": "2012-04-01 15:42:28",
                            "language": "jpn",
                            "handler_name": "GPAC ISO Audio Handler"
                        }
                    }
                ],
                "format": {
                    "filename": "lolwut.mp4",
                    "nb_streams": 2,
                    "format_name": "mov,mp4,m4a,3gp,3g2,mj2",
                    "format_long_name": "QuickTime/MPEG-4/Motion JPEG 2000 format",
                    "start_time": "0.000000",
                    "duration": "1239.195000",
                    "size": "296323860",
                    "bit_rate": "1913008",
                    "tags": {
                        "major_brand": "isom",
                        "minor_version": "1",
                        "compatible_brands": "isom",
                        "creation_time": "2012-04-01 15:42:24"
                    }
                }
            }                     
            """;
        // act
        VideoMetaInfo videoMetaInfo= VideoService.buildVideoInfoFromFfmpegJson(input);
        // assert
        assertThat(videoMetaInfo.videoCodec()).isEqualTo("h264");
        assertThat(videoMetaInfo.audioCodec()).isEqualTo("aac");
        assertThat(videoMetaInfo.durationSeconds()).isEqualTo(1239);
        assertThat(videoMetaInfo.resolution()).isEqualTo("1280x720");
        assertThat(videoMetaInfo.framesPerSecond()).isEqualTo(23);
    }
}