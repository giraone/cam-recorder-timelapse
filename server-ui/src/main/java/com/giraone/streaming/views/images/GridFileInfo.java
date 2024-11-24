package com.giraone.streaming.views.images;

import com.giraone.streaming.service.FileViewService;
import com.giraone.streaming.service.model.FileInfo;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.dataview.GridDataView;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class GridFileInfo extends Grid<FileInfo> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridFileInfo.class);

    private final FileInfoDataProvider fileInfoDataProvider;
    private final ConfigurableFilterDataProvider<FileInfo, Void, String> filterDataProvider;
    private final GridDataView<FileInfo> dataView;

    public GridFileInfo(boolean video, FileViewService fileViewService,
                        Consumer<FileInfo> displayFile,
                        Consumer<FileInfo> deleteFile,
                        Consumer<FileInfo> renameFile,
                        Consumer<Set<FileInfo>> selectionChangedListener,
                        Consumer<Integer> itemCountChangedConsumer) {

        super();
        this.fileInfoDataProvider = new FileInfoDataProvider(video, fileViewService);
        this.filterDataProvider = fileInfoDataProvider.withConfigurableFilter();

        this.addClassNames(video ? "videos-grid" : "images-grid");
        this.setSizeFull();
        this.setSelectionMode(Grid.SelectionMode.MULTI);
        final GridMultiSelectionModel<FileInfo> selectionModel = (GridMultiSelectionModel<FileInfo>) this.getSelectionModel();
        selectionModel.setDragSelect(true);
        if (selectionChangedListener != null) {
            this.addSelectionListener(selection -> selectionChangedListener.accept(selection.getAllSelectedItems()));
        }
        this.setPageSize(100);

        this.removeAllColumns();
        this.addComponentColumn(fileInfo -> {
            final Button displayButton = new Button("");
            displayButton.setIcon(LineAwesomeIcon.LAPTOP_SOLID.create());
            displayButton.addClickListener(event -> displayFile.accept(fileInfo));
            final Button deleteButton = new Button("");
            deleteButton.setIcon(LineAwesomeIcon.CUT_SOLID.create());
            deleteButton.addClickListener(event -> deleteFile.accept(fileInfo));
            final Button renameButton = new Button("");
            renameButton.setIcon(LineAwesomeIcon.PEN_SOLID.create());
            renameButton.addClickListener(event -> renameFile.accept(fileInfo));
            HorizontalLayout ret = new HorizontalLayout(displayButton, deleteButton, renameButton);
            ret.setWidth(64, Unit.PIXELS);
            return ret;
        }).setHeader("Action").setAutoWidth(false);
        this.addComponentColumn(fileInfo -> {
            final Image image = new Image(fileViewService.getThumbUrl(fileInfo), "no thubnail!");
            image.setWidth(64, Unit.PIXELS);
            image.setHeight(48, Unit.PIXELS);
            image.setClassName("no-padding");
            return image;
        }).setHeader("Image").setAutoWidth(false);
        this.addColumn(FileInfo::fileName, "fileName").setHeader("File Name").setAutoWidth(true);
        this.addColumn(FileInfo::toDisplayShort, "lastModified").setHeader("Last Modified").setAutoWidth(true);
        this.addColumn(FileInfo::sizeInBytes, "sizeInBytes").setHeader("Size").setAutoWidth(true);
        this.addColumn(FileInfo::infos, "infos").setHeader("Info").setAutoWidth(true);
        this.setMultiSort(false);
        this.sort(List.of(new GridSortOrder<>(this.getColumns().get(3), SortDirection.DESCENDING))); // lastModified

        dataView = this.setItems(filterDataProvider);
        if (itemCountChangedConsumer != null) {
            dataView.addItemCountChangeListener(e -> itemCountChangedConsumer.accept(e.getItemCount()));
        }
        //dataView.setItemCountEstimate(1000);
        //dataView.setItemCountEstimateIncrease(500);
    }

    public void setFilter(String filter) {
        LOGGER.info("setFilter \"{}\"", filter);
        filterDataProvider.setFilter(filter);
    }

    public void clearSelection() {
        this.asMultiSelect().clear();
    }

    public List<FileInfo> getItems() {
        return fileInfoDataProvider.getItems();
    }

    public boolean itemsIsEmpty() {
        return this.fileInfoDataProvider.itemsIsEmpty();
    }

    public int getTotalCount() {
        return this.fileInfoDataProvider.getTotalCount();
    }

    public int getItemsSize() {
        return this.fileInfoDataProvider.getItemsSize();
    }

    public int getDataViewSize() {
        return (int) this.dataView.getItems().count();
    }
}