# GridFileDownloader

Note: Proper functionality of this add-on requires the use of push or pull or some other way to update the state changes to the client-side when processing is done.

Known issues:
-- 

* There is no proper warning for missing push/pull.
* In single-select mode clicking the download cell also selects the row.
* If the order or count of columns or selection mode is changed the method recalculateDownloadColumn() must be called explicitly
* If the download column is rendered so that each cell contains e.g. a button that doesn't fill the whole cell, clicking the cell outside the button still triggers the download.


