package com.giraone.streaming.views.files;

import com.giraone.streaming.config.ApplicationProperties;
import com.giraone.streaming.service.FileViewService;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@SpringComponent
@Scope("prototype")
@PermitAll
@Route(value = "", layout = MainLayout.class)
@PageTitle("Files | Cam Recorder")
public class FilesView extends VerticalLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesView.class);

    private final Grid<FileInfo> grid = new Grid<>(FileInfo.class);
    private final TextField filterText = new TextField();
    private VerticalLayout displayForm;
    private Image displayImage;
    private Paragraph displayLabel;

    private final FileViewService fileViewService;
    private final ApplicationProperties applicationProperties;

    private List<FileInfo> items = List.of();
    private FileInfo currentItem = null;

    public FilesView(FileViewService fileViewService, ApplicationProperties applicationProperties) {

        this.fileViewService = fileViewService;
        this.applicationProperties = applicationProperties;
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureDisplay();
        add(getToolbar(), getContent());
        updateList();
        closeFileViewer();
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, displayForm);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, displayForm);
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        grid.addClassNames("files-grid");
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addSelectionListener(selection -> {
            LOGGER.debug("Number of selected files: {}", selection.getAllSelectedItems().size());
        });
        grid.removeAllColumns();
        grid.addComponentColumn(fileInfo -> {
            final Button displayButton = new Button("");
            displayButton.setIcon(LineAwesomeIcon.LAPTOP_SOLID.create());
            displayButton.addClickListener(event -> displayFile(fileInfo));
            final Button deleteButton = new Button("");
            deleteButton.setIcon(LineAwesomeIcon.CUT_SOLID.create());
            deleteButton.addClickListener(event -> deleteFile(fileInfo));
            HorizontalLayout ret = new HorizontalLayout(displayButton, deleteButton);
            ret.setWidth(64, Unit.PIXELS);
            return ret;
        }).setHeader("Action").setAutoWidth(false);
        grid.addComponentColumn(fileInfo -> {
            final Image image = new Image(fileViewService.getThumbUrl(fileInfo.fileName()), "no thubnail!");
            image.setWidth(64, Unit.PIXELS);
            image.setHeight(48, Unit.PIXELS);
            image.setClassName("no-padding");
            return image;
        }).setHeader("Image").setAutoWidth(false);
        grid.addColumn(FileInfo::fileName).setSortable(true).setHeader("File Name").setAutoWidth(true);
        grid.addColumn(FileInfo::toDisplayShort).setSortable(true).setHeader("Last Modified").setAutoWidth(true);
        grid.addColumn(FileInfo::sizeInBytes).setSortable(true).setHeader("Size").setAutoWidth(true);
        grid.addColumn(FileInfo::resolution).setSortable(true).setHeader("Resolution").setAutoWidth(true);
        //grid.addColumn(FileInfo::mediaType).setSortable(true).setHeader("Type").setAutoWidth(true);
        grid.sort(List.of(new GridSortOrder<>(grid.getColumns().get(4), SortDirection.DESCENDING))); // lastModified
    }

    private void configureDisplay() {
        final Button fullButton = new Button("Maximize View");
        fullButton.setClassName("no-padding");
        fullButton.setIcon(LineAwesomeIcon.CARET_SQUARE_LEFT_SOLID.create());
        fullButton.addClickListener(event -> fullFileViewer());
        final Button closeButton = new Button("Close");
        closeButton.setClassName("no-padding");
        closeButton.setIcon(LineAwesomeIcon.TIMES_CIRCLE_SOLID.create());
        closeButton.addClickListener(event -> closeFileViewer());
        final Button previousButton = new Button("Previous");
        previousButton.setClassName("no-padding");
        previousButton.setIcon(LineAwesomeIcon.BACKWARD_SOLID.create());
        previousButton.addClickListener(event -> viewPreviousFile());
        final Button nextButton = new Button("Next");
        nextButton.setClassName("no-padding");
        nextButton.setIcon(LineAwesomeIcon.FORWARD_SOLID.create());
        nextButton.addClickListener(event -> viewNextFile());
        displayImage = new Image("images/default-thumbnail.png", "");
        displayLabel = new Paragraph("-");
        displayForm = new VerticalLayout(
            new HorizontalLayout(fullButton, closeButton, previousButton, nextButton),
            displayLabel,
            displayImage
        );
        displayForm.setWidth("65em");
        displayForm.setVisible(false);
    }

    private Component getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button reloadButton = new Button("Reload");
        reloadButton.setIcon(LineAwesomeIcon.SYNC_SOLID.create());
        reloadButton.addClickListener(click -> updateList());

        Button deleteSelectedButton = new Button("Delete selected");
        deleteSelectedButton.setIcon(LineAwesomeIcon.CUT_SOLID.create());
        deleteSelectedButton.addClickListener(click -> deleteSelected());

        var toolbar = new HorizontalLayout(filterText, reloadButton, deleteSelectedButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    //-- actions --

    private void deleteFile(FileInfo fileInfo) {
        confirm("Delete file \"" + fileInfo.fileName() + "\"?", () -> deleteFileConfirmed(fileInfo));
    }

    private String deleteFileConfirmed(FileInfo fileInfo) {
        try {
            fileViewService.deleteFile(fileInfo);
        } catch (Exception e) {
            LOGGER.warn("deleteFile {} failed!", fileInfo, e);
            return e.getMessage();
        }
        updateList();
        return null;
    }

    private void deleteSelected() {
        Set<FileInfo> selectedItems = grid.getSelectedItems();
        confirm("Delete " + selectedItems.size() + " selected files?", () -> deleteSelectedConfirm(selectedItems));
    }

    private String deleteSelectedConfirm(Set<FileInfo> items) {
        try {
            fileViewService.deleteFiles(items);
        } catch (Exception e) {
            LOGGER.warn("deleteSelected failed!", e);
            return e.getMessage();
        }
        grid.asMultiSelect().clear();
        updateList();
        return null;
    }

    private void updateList() {
        items = fileViewService.listFileInfos(filterText.getValue());
        grid.setItems(items);
    }

    private void displayFile(FileInfo fileInfo) {
        currentItem = fileInfo;
        String url = applicationProperties.getHostUrl() + "/camera-images/" + fileInfo.fileName();
        openFileViewer(url, fileInfo.fileName() + "  (" + fileInfo.resolution() + ", " + fileInfo.sizeInBytes() + " Bytes)");
    }

    private void openFileViewer(String url, String label) {
        displayImage.setSrc(url);
        displayForm.setVisible(true);
        displayLabel.setText(label);
        addClassName("editing");
    }

    private void viewPreviousFile() {
        if (currentItem == null || currentItem.index() == 0) {
            return;
        }
        FileInfo fileInfo = items.get(currentItem.index() - 1);
        displayFile(fileInfo);
    }

    private void viewNextFile() {
        if (currentItem == null || currentItem.index() == items.size() - 1) {
            return;
        }
        FileInfo fileInfo = items.get(currentItem.index() + 1);
        displayFile(fileInfo);
    }

    private void closeFileViewer() {
        displayImage.setSrc("images/default-thumbnail.png");
        displayForm.setVisible(false);
        grid.setVisible(true);
        removeClassName("editing");
    }

    private void fullFileViewer() {
        grid.setVisible(false);
    }

    private void confirm(String text, Supplier<String> action) {
        final ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm");
        dialog.setText(text);
        dialog.setCancelable(true);
        dialog.setCancelText("No");
        dialog.addCancelListener(event -> dialog.close());
        dialog.setConfirmText("Yes");
        dialog.addConfirmListener(event -> {
            String result = action.get();
            if (result != null) {
                Notification.show(result);
            }
            dialog.close();
        });
        dialog.open();
    }
}
