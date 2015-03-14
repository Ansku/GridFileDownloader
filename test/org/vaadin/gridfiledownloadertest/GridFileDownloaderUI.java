/*
 * Copyright 2015 Vaadin Ltd.
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
package org.vaadin.gridfiledownloadertest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.servlet.annotation.WebServlet;

import org.vaadin.gridfiledownloader.GridFileDownloader;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Push
@SuppressWarnings("serial")
@Theme("gridfiledownloader")
public class GridFileDownloaderUI extends UI {

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = GridFileDownloaderUI.class, widgetset = "org.vaadin.gridfiledownloader.GridFileDownloaderWidgetset")
    public static class Servlet extends VaadinServlet {
    }

    private DownloadPojo downloadPojo;

    @SuppressWarnings("unchecked")
    @Override
    protected void init(VaadinRequest request) {
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        setContent(layout);

        final Grid grid = new Grid("Attachment grid");
        grid.setHeightMode(HeightMode.ROW);
        grid.setHeightByRows(5);
        // grid.setSelectionMode(SelectionMode.NONE);
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addColumn("filename");
        grid.getColumn("filename").setHeaderCaption("File name");
        grid.getColumn("filename").setExpandRatio(1);

        Indexed dataSource = grid.getContainerDataSource();
        for (int i = 1; i <= 5; ++i) {
            DownloadPojo cp = new DownloadPojo(i);
            Item item = dataSource.addItem(cp);
            item.getItemProperty("filename").setValue(cp.getName());
        }
        layout.addComponent(grid);
        addGridFileDownloader(grid);
    }

    /**
     * Adds a GridFileDownloader extension that adds a download column to the
     * Grid since no existing propertyId is specified.
     *
     * @param grid
     */
    private void addGridFileDownloader(Grid grid) {
        new GridFileDownloader(grid,
                new GridFileDownloader.GridStreamResource() {

                    @Override
                    public InputStream getStream() {
                        byte[] data = downloadPojo.getData();
                        if (data == null) {
                            return new ByteArrayInputStream(new byte[0]);
                        }
                        return new ByteArrayInputStream(data);
                    }

                    @Override
                    public String getFilename() {
                        return downloadPojo.getName();
                    }

                }) {

            @Override
            protected void setRowId(Object rowId) {
                try {
                    downloadPojo = (DownloadPojo) rowId;
                    super.setRowId(rowId);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(
                            "RowId must be DownloadPojo", e);
                }
            }
        };
    }

    public class DownloadPojo implements Serializable {
        private static final long serialVersionUID = 1L;

        String name;

        public DownloadPojo(int selectedRow) {
            name = "file " + selectedRow + ".txt";
        }

        public byte[] getData() {
            int writeAtOnce = 1024 * 1024 * 1024;
            byte[] b = new byte[writeAtOnce];
            return b;
        }

        public String getName() {
            return name;
        }

    }

}