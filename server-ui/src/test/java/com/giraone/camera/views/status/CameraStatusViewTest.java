package com.giraone.camera.views.status;

import com.giraone.camera.config.ApplicationProperties;
import com.giraone.camera.service.FileViewService;
import com.giraone.camera.service.model.CameraStatusRecord;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the "Camera Status" view. A test double of {@link FileViewService} replaces the backend server,
 * so the view can be built without a running server.
 */
class CameraStatusViewTest {

    private static final CameraStatusRecord OLDEST = record("2026-09-06T10:00:00");
    private static final CameraStatusRecord MIDDLE = record("2026-09-06T11:00:00");
    private static final CameraStatusRecord NEWEST = record("2026-09-06T12:00:00");

    @Test
    void oneTableIsDisplayedPerCamera() {
        // arrange
        FileViewServiceDouble fileViewService = new FileViewServiceDouble();
        fileViewService.add("cam-a", List.of(OLDEST));
        fileViewService.add("cam-b", List.of(NEWEST, MIDDLE));
        // act
        CameraStatusView view = new CameraStatusView(fileViewService);
        // assert
        assertThat(headersOf(view)).containsExactly("cam-a (1)", "cam-b (2)");
        assertThat(gridsOf(view)).hasSize(2);
    }

    @Test
    void allAttributesOfTheStatusRecordAreDisplayedAsColumns() {
        // arrange
        FileViewServiceDouble fileViewService = new FileViewServiceDouble();
        fileViewService.add("cam-a", List.of(NEWEST));
        // act
        CameraStatusView view = new CameraStatusView(fileViewService);
        // assert
        assertThat(gridsOf(view).getFirst().getColumns()).hasSize(CameraStatusRecord.class.getRecordComponents().length);
    }

    @Test
    void theRecordsAreOrderedByTimestampDescending() {
        // arrange
        FileViewServiceDouble fileViewService = new FileViewServiceDouble();
        fileViewService.add("cam-a", List.of(MIDDLE, OLDEST, NEWEST));
        // act
        CameraStatusView view = new CameraStatusView(fileViewService);
        // assert
        assertThat(gridsOf(view).getFirst().getListDataView().getItems())
            .containsExactly(NEWEST, MIDDLE, OLDEST);
    }

    @Test
    void aHintIsDisplayedWhenNoCameraReportedAStatus() {
        // arrange
        FileViewServiceDouble fileViewService = new FileViewServiceDouble();
        // act
        CameraStatusView view = new CameraStatusView(fileViewService);
        // assert
        assertThat(gridsOf(view)).isEmpty();
        assertThat(componentsOf(view, Paragraph.class).stream().map(Paragraph::getText))
            .contains("No camera has reported a status yet.");
    }

    @Test
    void anUnreachableBackendServerIsReportedInTheView() {
        // arrange
        FileViewServiceDouble fileViewService = new FileViewServiceDouble();
        fileViewService.failWith(new IllegalStateException("connection refused"));
        // act
        CameraStatusView view = new CameraStatusView(fileViewService);
        // assert
        assertThat(gridsOf(view)).isEmpty();
        assertThat(componentsOf(view, Paragraph.class).stream().map(Paragraph::getText))
            .contains("Cannot load the camera names: connection refused");
    }

    //------------------------------------------------------------------------------------------------------------------

    private static CameraStatusRecord record(String timestamp) {
        return new CameraStatusRecord(LocalDateTime.parse(timestamp), -60, 1, 2, 3, 4, 5, 6);
    }

    private static List<String> headersOf(CameraStatusView view) {
        return componentsOf(view, H3.class).stream().map(H3::getText).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<Grid<CameraStatusRecord>> gridsOf(CameraStatusView view) {
        return componentsOf(view, Grid.class).stream()
            .map(grid -> (Grid<CameraStatusRecord>) grid)
            .toList();
    }

    /** Collects all components of the given type in the component tree of the view, depth first. */
    private static <T extends Component> List<T> componentsOf(Component parent, Class<T> type) {
        return flatten(parent).filter(type::isInstance).map(type::cast).toList();
    }

    private static Stream<Component> flatten(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(CameraStatusViewTest::flatten));
    }

    /** Test double returning fixed camera names and status records instead of calling the backend server. */
    private static class FileViewServiceDouble extends FileViewService {

        private final Map<String, List<CameraStatusRecord>> statusByCamera = new HashMap<>();
        private RuntimeException failure;

        FileViewServiceDouble() {
            super(new ApplicationProperties());
        }

        void add(String cameraName, List<CameraStatusRecord> records) {
            statusByCamera.put(cameraName, records);
        }

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public List<String> listCameras() {
            if (failure != null) {
                throw failure;
            }
            return statusByCamera.keySet().stream().sorted().toList();
        }

        @Override
        public List<CameraStatusRecord> listCameraStatus(String cameraName) {
            return statusByCamera.getOrDefault(cameraName, List.of());
        }
    }
}
