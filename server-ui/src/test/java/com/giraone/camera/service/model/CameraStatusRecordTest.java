package com.giraone.camera.service.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CameraStatusRecordTest {

    @ParameterizedTest
    @CsvSource({
        "2026-09-06T12:30:45, 2026-09-06T12:30:45",
        "2026-01-02T03:04:05.678, 2026-01-02T03:04:05.678"
    })
    void timestampToDisplayReturnsIsoLocalDateTime(String timestamp, String expected) {
        // arrange
        CameraStatusRecord cameraStatusRecord = recordAt(LocalDateTime.parse(timestamp));
        // act
        String result = cameraStatusRecord.timestampToDisplay();
        // assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void timestampToDisplayReturnsPlaceholderForMissingTimestamp() {
        // arrange
        CameraStatusRecord cameraStatusRecord = recordAt(null);
        // act
        String result = cameraStatusRecord.timestampToDisplay();
        // assert
        assertThat(result).isEqualTo("-");
    }

    @Test
    void comparatorOrdersByTimestampDescending() {
        // arrange
        CameraStatusRecord oldest = recordAt(LocalDateTime.parse("2026-09-06T10:00:00"));
        CameraStatusRecord middle = recordAt(LocalDateTime.parse("2026-09-06T11:00:00"));
        CameraStatusRecord newest = recordAt(LocalDateTime.parse("2026-09-06T12:00:00"));
        List<CameraStatusRecord> records = new ArrayList<>(List.of(middle, oldest, newest));
        // act
        records.sort(CameraStatusRecord.BY_TIMESTAMP_DESCENDING);
        // assert
        assertThat(records).containsExactly(newest, middle, oldest);
    }

    @Test
    void comparatorPlacesRecordsWithoutTimestampAtTheEnd() {
        // arrange
        CameraStatusRecord withoutTimestamp = recordAt(null);
        CameraStatusRecord withTimestamp = recordAt(LocalDateTime.parse("2026-09-06T10:00:00"));
        List<CameraStatusRecord> records = new ArrayList<>(List.of(withoutTimestamp, withTimestamp));
        // act
        records.sort(CameraStatusRecord.BY_TIMESTAMP_DESCENDING);
        // assert
        assertThat(records).containsExactly(withTimestamp, withoutTimestamp);
    }

    private static CameraStatusRecord recordAt(LocalDateTime timestamp) {
        return new CameraStatusRecord(timestamp, -60, 1, 2, 3, 4, 5, 6);
    }
}
