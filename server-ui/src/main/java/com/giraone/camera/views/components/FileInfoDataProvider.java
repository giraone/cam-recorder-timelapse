package com.giraone.camera.views.components;

import com.giraone.camera.service.FileViewService;
import com.giraone.camera.service.model.FileInfo;
import com.giraone.camera.service.model.FileInfoOrder;
import com.giraone.camera.service.model.FileInfoQuery;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileInfoDataProvider extends AbstractBackEndDataProvider<FileInfo, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileInfoDataProvider.class);
    private static final FileInfoOrder DEFAULT_ORDER = new FileInfoOrder("fileName", false);

    private final boolean videos;
    private final FileViewService fileViewService;

    private List<FileInfo> items = new ArrayList<>();
    private int totalCount = 0;

    public FileInfoDataProvider(boolean videos, FileViewService fileViewService) {
        this.videos = videos;
        this.fileViewService = fileViewService;
    }

    @Override
    protected Stream<FileInfo> fetchFromBackEnd(Query<FileInfo, String> query) {
        final FileInfoQuery fileInfoQuery = fromVaadinQuery(query);
        items = videos
            ? fileViewService.listVideoInfos(fileInfoQuery)
            : fileViewService.listImageInfos(fileInfoQuery);
        LOGGER.info("FileInfoDataProvider.fetchFromBackEnd: fetched {} items", items.size());
        return items.stream();
    }

    @Override
    protected int sizeInBackEnd(Query<FileInfo, String> query) {
        final FileInfoQuery fileInfoQuery = fromVaadinQuery(query);
        totalCount = videos
            ? fileViewService.countVideoInfos(fileInfoQuery)
            : fileViewService.countImageInfos(fileInfoQuery);
        LOGGER.info("FileInfoDataProvider.sizeInBackEnd: totalCount={}", totalCount);
        return totalCount;
    }

    public List<FileInfo> getItems() {
        return items;
    }

    public boolean itemsIsEmpty() {
        return items.isEmpty();
    }

    public int getItemsSize() {
        return items.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    //------------------------------------------------------------------------------------------------------------------

    private static FileInfoQuery fromVaadinQuery(Query<FileInfo, String> query) {

        final List<QuerySortOrder> querySortOrders = query.getSortOrders();
        final QuerySortOrder querySortOrder = querySortOrders.isEmpty() ? null : querySortOrders.get(0);
        final FileInfoOrder order;
        if (querySortOrder == null) {
            LOGGER.debug("FileInfoDataProvider.fromVaadinQuery: No sort order!");
            order = DEFAULT_ORDER;
        } else {
            order = new FileInfoOrder(querySortOrder.getSorted(), querySortOrder.getDirection().equals(SortDirection.DESCENDING));
        }
        LOGGER.debug("FileInfoDataProvider.fromVaadinQuery: Sort order = {}, filter = {}", order, query.getFilter());
        return new FileInfoQuery(query.getFilter().orElse(null), query.getOffset(), query.getLimit(), order);
    }
}

