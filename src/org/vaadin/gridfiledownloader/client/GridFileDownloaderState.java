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
package org.vaadin.gridfiledownloader.client;

import com.vaadin.shared.AbstractComponentState;

public class GridFileDownloaderState extends AbstractComponentState {

    public Integer downloadColumnIndex = null;
    public Integer openColumnIndex = null;
    public boolean processing = false; // toggle to signal end of processing
    public int notificationDelay = -1;
    public String processingNotificationType = "humanized";
    public String processingCaption = "Processing previous download request.";
    public String processingDescription = "Please try again in a moment.";
    public String failureNotificationType = "humanized";
    public String failureCaption = "Download failed or was cancelled.";
    public String failureDescription = null;

}
