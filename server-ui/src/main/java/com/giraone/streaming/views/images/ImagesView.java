package com.giraone.streaming.views.images;

import com.giraone.streaming.config.ApplicationProperties;
import com.giraone.streaming.service.FileViewService;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.service.model.Status;
import com.giraone.streaming.service.model.timelapse.TimelapseCommand;
import com.giraone.streaming.service.model.timelapse.TimelapseResult;
import com.giraone.streaming.views.MainLayout;
import com.giraone.streaming.views.components.GridFileInfo;
import com.giraone.streaming.views.components.TextPromptDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@SpringComponent
@Scope("prototype")
@PermitAll
@Route(value = "", layout = MainLayout.class)
@PageTitle("Images | Cam Recorder")
public class ImagesView extends VerticalLayout {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagesView.class);

    private final GridFileInfo gridFileInfo;
    private final TextField filterText = new TextField();
    private VerticalLayout gridWithToolbar;
    private VerticalLayout displayForm;
    private Button fullButton;
    private Button halfButton;
    private Button previousButton;
    private Button nextButton;
    private Button deleteSelectedButton;
    private Button makeVideoButton;
    private Paragraph itemsLabel;

    private final FileViewService fileViewService;
    private final ApplicationProperties applicationProperties;

    private FileInfo currentItem = null;
    private boolean firstDisplay = true;

    public ImagesView(FileViewService fileViewService, ApplicationProperties applicationProperties) {

        this.fileViewService = fileViewService;
        this.applicationProperties = applicationProperties;
        this.gridFileInfo = new GridFileInfo(
            false,
            fileViewService,
            this::displayFile,
            this::deleteFile,
            this::renameFile,
            selectedItems -> activation(!selectedItems.isEmpty()),
            itemCount -> activation(false)
        );
        addClassName("images-view");
        setSizeFull();
        configureDisplay();
        add(getContent());
        closeFileViewer();
    }

    private HorizontalLayout getContent() {
        gridWithToolbar = new VerticalLayout(buildToolbar(), gridFileInfo);
        HorizontalLayout content = new HorizontalLayout(gridWithToolbar, displayForm);
        content.setFlexGrow(2, gridWithToolbar);
        content.setFlexGrow(1, displayForm);
        content.setSizeFull();
        return content;
    }

    private void configureDisplay() {
        fullButton = new Button("Maximize");
        fullButton.setClassName("no-padding");
        fullButton.setIcon(LineAwesomeIcon.CARET_SQUARE_LEFT_SOLID.create());
        fullButton.addClickListener(event -> fullFileViewer());
        fullButton.setEnabled(!gridFileInfo.itemsIsEmpty());
        halfButton = new Button("Normal");
        halfButton.setClassName("no-padding");
        halfButton.setIcon(LineAwesomeIcon.CARET_SQUARE_RIGHT_SOLID.create());
        halfButton.addClickListener(event -> halfFileViewer());
        halfButton.setVisible(false);
        final Button closeButton = new Button("Close");
        closeButton.setClassName("no-padding");
        closeButton.setIcon(LineAwesomeIcon.TIMES_CIRCLE_SOLID.create());
        closeButton.addClickListener(event -> closeFileViewer());
        previousButton = new Button("Previous");
        previousButton.setClassName("no-padding");
        previousButton.setIcon(LineAwesomeIcon.BACKWARD_SOLID.create());
        previousButton.addClickListener(event -> viewPreviousFile());
        previousButton.setEnabled(!gridFileInfo.itemsIsEmpty());
        nextButton = new Button("Next");
        nextButton.setClassName("no-padding");
        nextButton.setIcon(LineAwesomeIcon.FORWARD_SOLID.create());
        nextButton.addClickListener(event -> viewNextFile());
        nextButton.setEnabled(!gridFileInfo.itemsIsEmpty());
        IFrame displayIframe = new IFrame("components/image-viewer/image-viewer.html");
        displayIframe.setWidth("100%");
        displayIframe.setHeight("100vh");
        displayIframe.getElement().setAttribute("frameborder", "0");
        displayIframe.setId("imageViewerIFrame");
        displayForm = new VerticalLayout(
            new HorizontalLayout(fullButton, halfButton, closeButton, previousButton, nextButton),
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
        filterText.setMinWidth("20%");
        filterText.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList(e.getValue()));

        Button reloadButton = new Button("Reload");
        reloadButton.setIcon(LineAwesomeIcon.SYNC_SOLID.create());
        reloadButton.addClickListener(click -> updateList(filterText.getValue()));

        deleteSelectedButton = new Button("Delete");
        deleteSelectedButton.setIcon(LineAwesomeIcon.CUT_SOLID.create());
        deleteSelectedButton.addClickListener(click -> deleteSelected());
        deleteSelectedButton.setEnabled(!gridFileInfo.itemsIsEmpty());

        Button downloadSelectedButton = new Button("Donwload");
        downloadSelectedButton.setIcon(LineAwesomeIcon.DOWNLOAD_SOLID.create());
        downloadSelectedButton.addClickListener(click -> downloadSelected());
        downloadSelectedButton.setEnabled(!gridFileInfo.itemsIsEmpty());

        makeVideoButton = new Button("Create video");
        makeVideoButton.setIcon(LineAwesomeIcon.VIDEO_SOLID.create());
        makeVideoButton.addClickListener(click -> makeTimelapseVideo());
        makeVideoButton.setEnabled(!gridFileInfo.itemsIsEmpty());

        itemsLabel = new Paragraph("0 of 0 items");
        itemsLabel.setMinWidth(150, Unit.PIXELS);

        // specific
        return new HorizontalLayout(filterText, reloadButton, deleteSelectedButton, makeVideoButton, itemsLabel);
    }

    //-- actions --

    private void renameFile(FileInfo fileInfo) {
        final TextPromptDialog textPromptDialog = new TextPromptDialog(
            "Rename", "New name:",
            fileInfo.fileName(), "New name",
            name -> renameFile(fileInfo, name));
        textPromptDialog.open();
    }

    private void renameFile(FileInfo fileInfo, String name) {
        try {
            Status ret = fileViewService.renameImage(fileInfo, name);
            if (!ret.success()) {
                showError("renameFile {} failed! " + ret.error());
            }
        } catch (Exception e) {
            LOGGER.warn("renameFile {} failed!", fileInfo, e);
            showError("renameFile {} failed! " + e.getMessage());
            return;
        }
        updateList(filterText.getValue());
    }

    private void deleteFile(FileInfo fileInfo) {
        confirm("Delete file \"" + fileInfo.fileName() + "\"?", () -> deleteFileConfirmed(fileInfo));
    }

    private String deleteFileConfirmed(FileInfo fileInfo) {
        try {
            Status ret = fileViewService.deleteImage(fileInfo);
            if (!ret.success()) {
                showError("deleteFile {} failed! " + ret.error());
            }
        } catch (Exception e) {
            LOGGER.warn("deleteFile {} failed!", fileInfo, e);
            return e.getMessage();
        }
        updateList(filterText.getValue());
        return null;
    }

    private void deleteSelected() {
        Set<FileInfo> selectedItems = gridFileInfo.getSelectedItems();
        confirm("Delete " + selectedItems.size() + " selected files?", () -> deleteSelectedConfirm(selectedItems));
    }

    private String deleteSelectedConfirm(Set<FileInfo> itemsToDelete) {
        try {
            fileViewService.deleteImages(itemsToDelete);
        } catch (Exception e) {
            LOGGER.warn("deleteSelected failed!", e);
            return e.getMessage();
        }
        gridFileInfo.clearSelection();
        updateList(filterText.getValue());
        return null;
    }

    private void updateList(String filterValue) {
        gridFileInfo.setFilter(filterValue);
        activation(false);
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
        String url = applicationProperties.getHostUrl() + "/images/" + fileInfo.fileName();
        openFileViewer(url, fileInfo.fileName() + "  (" + fileInfo.infos() + ", " + fileInfo.sizeInBytes() + " Bytes)");
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
        final int size = gridFileInfo.getItemsSize();
        if (size == 0) {
            return;
        }
        final List<FileInfo> sortedItems = gridFileInfo.getItems();
        final int currentIndex = findIndexOfItem(sortedItems, currentItem);
        final int nextIndex = currentIndex == 0 ? size - 1 : currentIndex - 1;
        final FileInfo fileInfo = sortedItems.get(nextIndex);
        displayFile(fileInfo);
    }

    private void viewNextFile() {
        final int size = gridFileInfo.getItemsSize();
        if (size == 0) {
            return;
        }
        final List<FileInfo> sortedItems = gridFileInfo.getItems();
        final int currentIndex = findIndexOfItem(sortedItems, currentItem);
        final int nextIndex = currentIndex == size - 1 ? 0 : currentIndex + 1;
        final FileInfo fileInfo = sortedItems.get(nextIndex);
        displayFile(fileInfo);
    }

    private void closeFileViewer() {
        displayForm.setVisible(false);
        gridWithToolbar.setVisible(true);
        fullButton.setEnabled(!gridFileInfo.itemsIsEmpty());
        fullButton.setVisible(true);
        removeClassName("editing");
    }

    private void fullFileViewer() {
        gridWithToolbar.setVisible(false);
        fullButton.setVisible(false);
        halfButton.setVisible(true);
    }

    private void halfFileViewer() {
        gridWithToolbar.setVisible(true);
        fullButton.setVisible(true);
        halfButton.setVisible(false);
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

    private void activation(boolean selected) {
        final int itemsSize = gridFileInfo.getItemsSize();
        final int totalCount = gridFileInfo.getTotalCount();
        LOGGER.warn("activation selected={} itemsSize={} totalCount={}", selected, itemsSize, totalCount);
        final boolean entries = itemsSize > 0;
        deleteSelectedButton.setEnabled(entries && selected);
        makeVideoButton.setEnabled(entries && selected);
        previousButton.setEnabled(entries);
        nextButton.setEnabled(entries);
        fullButton.setEnabled(entries);
        halfButton.setEnabled(entries);
        itemsLabel.setText(String.format("%d of %d items", itemsSize, totalCount));
    }

    private void downloadSelected() {
        Set<FileInfo> selectedItems = gridFileInfo.getSelectedItems();
        List<String> names = selectedItems.stream().map(FileInfo::fileName).sorted().toList();
        try {
            String result = fileViewService.downloadSelectedImages(names).block(Duration.ofSeconds(120));
            Notification notification = Notification.show(result);
            notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        } catch (Exception e) {
            LOGGER.warn("downloadSelected failed!", e);
            showError(e.getMessage());
        }
    }

    // specific
    private void makeTimelapseVideo() {
        Set<FileInfo> selectedItems = gridFileInfo.getSelectedItems();
        if (selectedItems.isEmpty()) {
            return;
        }
        List<String> names = selectedItems.stream().map(FileInfo::fileName).sorted().toList();
        String outputVideoName = names.get(0).replace(".jpg", ".mp4");
        TimelapseCommand timelapseCommand = new TimelapseCommand(outputVideoName, names, 1, 10);
        try {
            TimelapseResult result = fileViewService.makeTimelapseVideo(timelapseCommand).block(Duration.ofSeconds(120));
            if (result != null) {
                Notification notification = Notification.show(result.toString());
                notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            } else {
                showError("Timeout for makeTimelapseVideo!");
            }
        } catch (Exception e) {
            LOGGER.warn("makeTimelapseVideo failed!", e);
            showError(e.getMessage());
        }
    }

    private void showError(String text) {
        Notification notification = new Notification();
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        Div divText = new Div(new Text(text));
        Button closeButton = new Button(new Icon("lumo", "cross"));
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.setAriaLabel("Close");
        closeButton.addClickListener(event -> {
            notification.close();
        });
        HorizontalLayout layout = new HorizontalLayout(divText, closeButton);
        layout.setAlignItems(Alignment.CENTER);
        notification.add(layout);
        notification.open();
    }
}
