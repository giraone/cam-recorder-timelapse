package com.giraone.streaming.views.images;

import com.giraone.streaming.config.ApplicationProperties;
import com.giraone.streaming.service.FileViewService;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringComponent
@Scope("prototype")
@PermitAll
@Route(value = "", layout = MainLayout.class)
@PageTitle("Images | Cam Recorder")
public class ImagesView extends VerticalLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagesView.class);

    private final Grid<FileInfo> grid = new Grid<>(FileInfo.class);
    private final TextField filterText = new TextField();
    private VerticalLayout gridWithToolbar;
    private VerticalLayout displayForm;
    private Button fullButton;
    private Button previousButton;
    private Button nextButton;

    private final FileViewService fileViewService;
    private final ApplicationProperties applicationProperties;

    private List<FileInfo> items = List.of();
    private FileInfo currentItem = null;
    private boolean firstDisplay = true;

    public ImagesView(FileViewService fileViewService, ApplicationProperties applicationProperties) {

        this.fileViewService = fileViewService;
        this.applicationProperties = applicationProperties;
        addClassName("images-view");
        setSizeFull();
        configureGrid();
        configureDisplay();
        add(getContent());
        updateList();
        closeFileViewer();
    }

    private HorizontalLayout getContent() {
        gridWithToolbar = new VerticalLayout(buildToolbar(), grid);
        HorizontalLayout content = new HorizontalLayout(gridWithToolbar, displayForm);
        content.setFlexGrow(2, gridWithToolbar);
        content.setFlexGrow(1, displayForm);
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        grid.addClassNames("images-grid");
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addSelectionListener(selection -> {
            LOGGER.debug("Number of selected images: {}", selection.getAllSelectedItems().size());
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
            final Image image = new Image(fileViewService.getThumbUrl(fileInfo), "no thubnail!");
            image.setWidth(64, Unit.PIXELS);
            image.setHeight(48, Unit.PIXELS);
            image.setClassName("no-padding");
            return image;
        }).setHeader("Image").setAutoWidth(false);
        grid.addColumn(FileInfo::fileName).setSortable(true).setHeader("File Name").setAutoWidth(true);
        grid.addColumn(FileInfo::toDisplayShort).setSortable(true).setHeader("Last Modified").setAutoWidth(true);
        grid.addColumn(FileInfo::sizeInBytes).setSortable(true).setHeader("Size").setAutoWidth(true);
        grid.addColumn(FileInfo::resolution).setSortable(true).setHeader("Resolution").setAutoWidth(true);
        grid.sort(List.of(new GridSortOrder<>(grid.getColumns().get(3), SortDirection.DESCENDING))); // lastModified
    }

    private void configureDisplay() {
        fullButton = new Button("Maximize View");
        fullButton.setClassName("no-padding");
        fullButton.setIcon(LineAwesomeIcon.CARET_SQUARE_LEFT_SOLID.create());
        fullButton.addClickListener(event -> fullFileViewer());
        fullButton.setEnabled(!items.isEmpty());
        final Button closeButton = new Button("Close");
        closeButton.setClassName("no-padding");
        closeButton.setIcon(LineAwesomeIcon.TIMES_CIRCLE_SOLID.create());
        closeButton.addClickListener(event -> closeFileViewer());
        previousButton = new Button("Previous");
        previousButton.setClassName("no-padding");
        previousButton.setIcon(LineAwesomeIcon.BACKWARD_SOLID.create());
        previousButton.addClickListener(event -> viewPreviousFile());
        previousButton.setEnabled(!items.isEmpty());
        nextButton = new Button("Next");
        nextButton.setClassName("no-padding");
        nextButton.setIcon(LineAwesomeIcon.FORWARD_SOLID.create());
        nextButton.addClickListener(event -> viewNextFile());
        nextButton.setEnabled(!items.isEmpty());
        IFrame displayIframe = new IFrame("components/image-viewer/image-viewer.html");
        displayIframe.setWidth("100%");
        displayIframe.setHeight("100vh");
        displayIframe.getElement().setAttribute("frameborder", "0");
        displayIframe.setId("imageViewerIFrame");
        displayForm = new VerticalLayout(
            new HorizontalLayout(fullButton, closeButton, previousButton, nextButton),
            displayIframe
        );
        displayForm.setPadding(false);
        displayForm.setMinWidth("35%");
        displayForm.setVisible(false);
        UI.getCurrent().getPage().addJavaScript("js/global.js");
    }

    private Component buildToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setMinWidth("40%");
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button reloadButton = new Button("Reload");
        reloadButton.setIcon(LineAwesomeIcon.SYNC_SOLID.create());
        reloadButton.addClickListener(click -> updateList());

        Button deleteSelectedButton = new Button("Delete selected");
        deleteSelectedButton.setIcon(LineAwesomeIcon.CUT_SOLID.create());
        deleteSelectedButton.addClickListener(click -> deleteSelected());

        return new HorizontalLayout(filterText, reloadButton, deleteSelectedButton);
    }

    //-- actions --

    private void deleteFile(FileInfo fileInfo) {
        confirm("Delete file \"" + fileInfo.fileName() + "\"?", () -> deleteFileConfirmed(fileInfo));
    }

    private String deleteFileConfirmed(FileInfo fileInfo) {
        try {
            fileViewService.deleteImage(fileInfo);
        } catch (Exception e) {
            LOGGER.warn("deleteFile {} failed!", fileInfo, e);
            return e.getMessage();
        }
        updateList();
        return null;
    }

    private void deleteSelected() {
        Set<FileInfo> selectedItems = grid.getSelectedItems();
        confirm("Delete " + selectedItems.size() + " selected images?", () -> deleteSelectedConfirm(selectedItems));
    }

    private String deleteSelectedConfirm(Set<FileInfo> itemsToDelete) {
        try {
            fileViewService.deleteImages(itemsToDelete);
        } catch (Exception e) {
            LOGGER.warn("deleteSelected failed!", e);
            return e.getMessage();
        }
        grid.asMultiSelect().clear();
        updateList();
        return null;
    }

    private void updateList() {
        items = fileViewService.listImageInfos(filterText.getValue());
        grid.setItems(items);
        final boolean enabled = !items.isEmpty();
        previousButton.setEnabled(enabled);
        nextButton.setEnabled(enabled);
    }

    private List<FileInfo> getItemsWithSortOrder() {
        ListDataProvider<FileInfo> dataProvider = (ListDataProvider<FileInfo>) grid.getDataProvider();
        int totalSize = dataProvider.getItems().size();
        DataCommunicator<FileInfo> dataCommunicator = grid.getDataCommunicator();
        Stream<FileInfo> stream = dataProvider.fetch(new Query<>(
            0,
            totalSize,
            dataCommunicator.getBackEndSorting(),
            dataCommunicator.getInMemorySorting(),
            dataProvider.getFilter()));
        return stream.collect(Collectors.toList());
    }

    private static int findIndexOfItem(List<FileInfo> items, FileInfo wanted) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).fileName().equals(wanted.fileName())) {
                return i;
            }
        }
        return -1;
    }

    private void displayFile(FileInfo fileInfo) {
        currentItem = fileInfo;
        String url = applicationProperties.getHostUrl() + "/camera-images/" + fileInfo.fileName();
        openFileViewer(url, fileInfo.fileName() + "  (" + fileInfo.resolution() + ", " + fileInfo.sizeInBytes() + " Bytes)");
    }

    private void openFileViewer(String url, String label) {
        displayForm.setVisible(true);
        if (firstDisplay) {
            // for some reason on the first image, we have to wait a little before we can load the image
            UI.getCurrent().getPage().executeJs("setTimeout(loadImage,500,$0,$1)", url, label);
            firstDisplay = false;
        } else {
            UI.getCurrent().getPage().executeJs("loadImage($0,$1)", url, label);
        }
    }

    private void viewPreviousFile() {
        if (items.isEmpty()) {
            return;
        }
        final List<FileInfo> sortedItems = getItemsWithSortOrder();
        final int currentIndex = findIndexOfItem(sortedItems, currentItem);
        final int nextIndex = currentIndex == 0 ? items.size() - 1 : currentIndex - 1;
        final FileInfo fileInfo = sortedItems.get(nextIndex);
        displayFile(fileInfo);
    }

    private void viewNextFile() {
        if (items.isEmpty()) {
            return;
        }
        final List<FileInfo> sortedItems = getItemsWithSortOrder();
        final int currentIndex = findIndexOfItem(sortedItems, currentItem);
        final int nextIndex =currentIndex == items.size() - 1 ? 0 : currentIndex + 1;
        final FileInfo fileInfo = sortedItems.get(nextIndex);
        displayFile(fileInfo);
    }

    private void closeFileViewer() {
        displayForm.setVisible(false);
        gridWithToolbar.setVisible(true);
        fullButton.setEnabled(!items.isEmpty());
        removeClassName("editing");
    }

    private void fullFileViewer() {
        gridWithToolbar.setVisible(false);
        fullButton.setEnabled(false);
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
