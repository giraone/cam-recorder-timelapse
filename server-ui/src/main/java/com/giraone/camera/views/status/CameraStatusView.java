package com.giraone.camera.views.status;

import com.giraone.camera.service.FileViewService;
import com.giraone.camera.service.model.CameraStatusRecord;
import com.giraone.camera.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.List;

/**
 * View showing the status records of all cameras, that have reported a status to the backend server.
 * One table is displayed per camera, containing all attributes of {@link CameraStatusRecord}.
 * The records are ordered by timestamp in descending order, so the most recent status comes first.
 */
@SpringComponent
@Scope("prototype") // prototype scope generates a fresh object for every method call or dependency injection
@PermitAll
@Route(value = "camera-status", layout = MainLayout.class)
@PageTitle("Camera Status | Cam Recorder")
public class CameraStatusView extends VerticalLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraStatusView.class);

    /** Height of a single camera table. Tables with more records than this are scrollable. */
    private static final String GRID_HEIGHT = "20em";

    private final FileViewService fileViewService;
    private final VerticalLayout tables = new VerticalLayout();
    private final Paragraph camerasLabel = new Paragraph();

    public CameraStatusView(FileViewService fileViewService) {

        this.fileViewService = fileViewService;
        addClassName("camera-status-view");
        setSizeFull();
        tables.setPadding(false);
        tables.setSizeFull();
        add(buildToolbar(), tables);
        updateTables();
    }

    private Component buildToolbar() {

        final Button reloadButton = new Button("Reload");
        reloadButton.setIcon(LineAwesomeIcon.SYNC_SOLID.create());
        reloadButton.addClickListener(click -> updateTables());

        camerasLabel.setMinWidth(150, Unit.PIXELS);

        return new HorizontalLayout(reloadButton, camerasLabel);
    }

    /** Reloads the camera names and rebuilds one table per camera. */
    private void updateTables() {

        tables.removeAll();
        final List<String> cameraNames;
        try {
            cameraNames = fileViewService.listCameras();
        } catch (Exception e) {
            LOGGER.warn("Cannot load the camera names!", e);
            camerasLabel.setText("");
            tables.add(new Paragraph("Cannot load the camera names: " + e.getMessage()));
            return;
        }
        LOGGER.debug("updateTables cameras={}", cameraNames);
        camerasLabel.setText(cameraNames.size() == 1 ? "1 camera" : cameraNames.size() + " cameras");
        if (cameraNames.isEmpty()) {
            tables.add(new Paragraph("No camera has reported a status yet."));
            return;
        }
        cameraNames.forEach(cameraName -> tables.add(buildCameraTable(cameraName)));
    }

    private Component buildCameraTable(String cameraName) {

        final List<CameraStatusRecord> records = fileViewService.listCameraStatus(cameraName).stream()
            .sorted(CameraStatusRecord.BY_TIMESTAMP_DESCENDING)
            .toList();
        final H3 header = new H3(cameraName + " (" + records.size() + ")");
        final VerticalLayout ret = new VerticalLayout(header, buildGrid(records));
        ret.setPadding(false);
        ret.setWidthFull();
        return ret;
    }

    /**
     * Builds the table of one camera with a column for every attribute of {@link CameraStatusRecord}.
     * The grid is sorted by its timestamp column in descending order, so the initial ordering of the
     * records is kept visible, even though the user may re-sort the table by any column.
     */
    private Grid<CameraStatusRecord> buildGrid(List<CameraStatusRecord> records) {

        final Grid<CameraStatusRecord> grid = new Grid<>();
        grid.addClassName("camera-status-grid");
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setHeight(GRID_HEIGHT);
        grid.setMultiSort(false);

        final Grid.Column<CameraStatusRecord> timestampColumn = grid
            .addColumn(CameraStatusRecord::timestampToDisplay)
            .setComparator(CameraStatusRecord::timestamp)
            .setHeader("Timestamp").setAutoWidth(true);
        grid.addColumn(CameraStatusRecord::rssi).setHeader("RSSI (dBm)").setAutoWidth(true);
        grid.addColumn(CameraStatusRecord::imageCounter).setHeader("Image Counter").setAutoWidth(true);
        grid.addColumn(CameraStatusRecord::imageErrors).setHeader("Image Errors").setAutoWidth(true);
        grid.addColumn(CameraStatusRecord::cameraInitCounter).setHeader("Camera Init Counter").setAutoWidth(true);
        grid.addColumn(CameraStatusRecord::cameraInitErrors).setHeader("Camera Init Errors").setAutoWidth(true);
        grid.addColumn(CameraStatusRecord::uploadImageErrors).setHeader("Upload Image Errors").setAutoWidth(true);
        grid.addColumn(CameraStatusRecord::uploadStatusErrors).setHeader("Upload Status Errors").setAutoWidth(true);

        grid.setItems(records);
        grid.sort(List.of(new GridSortOrder<>(timestampColumn, SortDirection.DESCENDING)));
        return grid;
    }
}
