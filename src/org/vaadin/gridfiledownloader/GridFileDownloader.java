/*
 * Copyright 2015-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.gridfiledownloader;

import java.io.IOException;
import java.util.logging.Logger;

import org.vaadin.gridfiledownloader.client.GridFileDownloaderServerRpc;
import org.vaadin.gridfiledownloader.client.GridFileDownloaderState;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.data.Container.Indexed;
import com.vaadin.server.ConnectorResource;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.MultiSelectionModel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.renderers.HtmlRenderer;

/**
 * This specialises {@link FileDownloader} for grid so that both the file name
 * and content can be determined on-demand, i.e. when the user has clicked on
 * the download column of some row.
 */
@StyleSheet("gridfiledownloader.css")
public class GridFileDownloader extends FileDownloader {

    private static Logger getLogger() {
        return Logger.getLogger(GridFileDownloader.class.getName());
    }

    /**
     * Provide both the {@link StreamSource} and the filename in an on-demand
     * way.
     */
    public interface GridStreamResource extends StreamSource {
        String getFilename();
    }

    private static final long serialVersionUID = 1L;
    private final GridStreamResource gridStreamResource;
    private Grid grid;
    private Object downloadPropertyId;
    private Object rowId;
    private GridFileDownloaderServerRpc rpc = new GridFileDownloaderServerRpc() {

        @Override
        public void download(Integer rowIndex) {
            if (rowIndex != null) {
                setRowId(grid.getContainerDataSource().getIdByIndex(rowIndex));
            }
        }

    };

    /**
     * FileDownloader extension that adds a download column to the Grid. Note
     * that if the order or count of the columns or selection mode changes you
     * need to call {@link #recalculateDownloadColumn()} explicitly.
     *
     * @param grid
     * @param gridStreamResource
     */
    public GridFileDownloader(Grid grid, GridStreamResource gridStreamResource) {
        this(grid, null, gridStreamResource);
    }

    /**
     * FileDownloader extension that adds a download behaviour to the given
     * column of the Grid. Note that if the order or count of the columns or
     * selection mode change you need to call
     * {@link #recalculateDownloadColumn()} explicitly.
     *
     * @param grid
     * @param downloadPropertyId
     * @param gridStreamResource
     */
    public GridFileDownloader(Grid grid, Object downloadPropertyId,
            GridStreamResource gridStreamResource) {
        super(new StreamResource(gridStreamResource, ""));
        assert gridStreamResource != null : "The given on-demand stream resource may never be null!";
        assert grid != null : "The given grid may never be null!";

        this.gridStreamResource = gridStreamResource;
        registerRpc(rpc);
        extend(grid);
        if (downloadPropertyId == null) {
            addDownloadColumn();
        } else {
            setDownloadColumn(downloadPropertyId);
        }
        grid.setCellStyleGenerator(new Grid.CellStyleGenerator() {

            @Override
            public String getStyle(Grid.CellReference cellReference) {
                if (GridFileDownloader.this.downloadPropertyId
                        .equals(cellReference.getPropertyId())) {
                    return "gridfiledownloader-downloadcolumn";
                }
                return null;
            }
        });
    }

    @Override
    public boolean handleConnectorRequest(VaadinRequest request,
            VaadinResponse response, String path) throws IOException {

        if (!path.matches("dl(/.*)?")) {
            // Ignore if it isn't for us
            return false;
        }

        boolean markedProcessed = false;
        try {
            if (!waitForRPC()) {
                handleRPCTimeout();
                return false;
            }
            getResource().setFilename(gridStreamResource.getFilename());

            VaadinSession session = getSession();

            session.lock();
            DownloadStream stream;

            try {
                Resource resource = getFileDownloadResource();
                if (!(resource instanceof ConnectorResource)) {
                    return false;
                }
                stream = ((ConnectorResource) resource).getStream();

                if (stream.getParameter("Content-Disposition") == null) {
                    // Content-Disposition: attachment generally forces download
                    stream.setParameter("Content-Disposition",
                            "attachment; filename=\"" + stream.getFileName()
                                    + "\"");
                }

                // Content-Type to block eager browser plug-ins from hijacking
                // the file
                if (isOverrideContentType()) {
                    stream.setContentType("application/octet-stream;charset=UTF-8");
                }
            } finally {
                try {
                    markProcessed();
                    markedProcessed = true;
                } finally {
                    session.unlock();
                }
            }
            try {
                stream.writeResponse(request, response);
            } catch (Exception e) {
                handleWriteResponseException(e);
            }
            return true;
        } finally {
            // ensure the download request always gets marked processed
            if (!markedProcessed) {
                markProcessed();
            }
        }
    }

    protected void handleRPCTimeout() {
        markProcessed();
        getLogger().severe(
                "Download attempt timeout before receiving RPC call about row");
    }

    /**
     * Wait until RPC call has reached the server-side with the rowId.
     */
    protected boolean waitForRPC() {
        int counter = 0;
        while (counter < 30) {
            if (getRowId() != null) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            } finally {
                ++counter;
            }
        }
        return getRowId() != null;
    }

    /**
     * Override this method if you want more specific failure handling for write
     * response exceptions. The main point of this is to keep the exceptions
     * from e.g. user cancelling the download from showing up in tooltip for the
     * Grid.
     *
     * @param e
     */
    protected void handleWriteResponseException(Exception e) {
        e.printStackTrace();
        for (Type type : Type.values()) {
            if (type.getStyle().equals(getState().failureNotificationType)) {
                Notification.show(getState().failureCaption,
                        getState().failureDescription, type);
                return;
            }
        }
    }

    protected void markProcessed() {
        setRowId(null);
        getState().processing = !getState().processing;
        markAsDirty();
    }

    private StreamResource getResource() {
        return (StreamResource) getFileDownloadResource();
    }

    protected void setRowId(Object rowId) {
        this.rowId = rowId;
    }

    protected Object getRowId() {
        return rowId;
    }

    @Override
    protected GridFileDownloaderState getState() {
        return (GridFileDownloaderState) super.getState();
    }

    /**
     * DO NOT CALL THIS EXPLICITLY! The behaviour of this extension is not
     * guaranteed if the target changes from the default.
     */
    @Override
    public void extend(AbstractComponent target) {
        if (target instanceof Grid) {
            grid = (Grid) target;
            super.extend(target);
        } else {
            throw new IllegalArgumentException(
                    "Target must be instance of Grid");
        }
    }

    /**
     * Sets the download column. Note that thanks to the workaround for how
     * columns are recognised {@link #recalculateDownloadColumn()} must be
     * explicitly called every time column order or count or selection mode
     * changes.
     *
     * @param propertyId
     */
    protected void setDownloadColumn(Object propertyId) {
        downloadPropertyId = propertyId;
        recalculateDownloadColumn();
    }

    /**
     * <b>This method must be called every time column order or count or
     * selection mode changes</b>, otherwise download requests might get
     * calculated for the wrong column.
     */
    public void recalculateDownloadColumn() {
        getState().downloadColumnIndex = grid.getColumns().indexOf(
                grid.getColumn(downloadPropertyId));
        if (grid.getSelectionModel() instanceof MultiSelectionModel) {
            // MultiSelection adds extra column to the grid
            ++getState().downloadColumnIndex;
        }
    }

    /**
     * Adds a download column with propertyId {@link FontAwesome#DOWNLOAD} to
     * the Grid and registers it with this extension.
     *
     * @see GridFileDownloader#setDownloadColumn(Object)
     */
    protected void addDownloadColumn() {
        Indexed dataSource = grid.getContainerDataSource();
        FontAwesome icon = FontAwesome.DOWNLOAD;
        dataSource.addContainerProperty(icon, String.class,
                createDownloadHtml());
        grid.getColumn(icon).setRenderer(new HtmlRenderer());
        grid.getHeaderRow(0).getCell(icon).setHtml(createDownloadHtml());
        grid.getColumn(icon).setSortable(false);
        setDownloadColumn(icon);
    }

    /**
     * Creates the HTML content of the generated download column.
     *
     * @return HTML content as String
     */
    protected String createDownloadHtml() {
        return FontAwesome.DOWNLOAD.getHtml();
    }
}
