package com.jwdownload.upgrade;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Observable;
 
// This class downloads a file from a URL.
@Data
class Download extends Observable implements Runnable {
     
    // Max size of download buffer.
    private static final int MAX_BUFFER_SIZE = 1024;

    protected static final Logger logger = LoggerFactory.getLogger(Download.class);

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT =  new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");


    @Getter
    private URL urlToDownload; // download URL
    @Getter
    @Setter
    private String fileName; // download URL

    private int size = -1; // size of download in bytes
    private int downloaded = -1; // number of bytes downloaded
    private STATUS_DOWNLOAD status; // current status of download
    @Getter
    @Setter
    private RandomAccessFile file = null;

    private String folderToSave ;

    private File fileSaved ;

    private boolean start = false;
     
    // Constructor for Download.
    public Download(URL url, String folder, String fileName) {
        this.urlToDownload = url;
        size = -1;
        downloaded = 0;
        status = STATUS_DOWNLOAD.DOWNLOADING;
        folderToSave = folder;
        setFileName(fileName);
        statusAndSize();
    }

    private void statusAndSize() {
        InputStream stream = null;
        fileSaved = new File(folderToSave + "/" + getFileName());
        try {
            URL url2 = getUrlToDownload();
            // Open connection to URL.
            HttpURLConnection connection =
                    (HttpURLConnection) url2.openConnection();
            // Specify what portion of file to download.
            connection.setRequestProperty("Range",
                    "bytes=" + downloaded + "-");

            // Connect to server.
            connection.connect();

            // Make sure response code is in the 200 range.
            if (connection.getResponseCode() / 100 != 2) {
                error();
            }
            // Check for valid content length.
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }
            size = contentLength;
            if(fileSaved.exists() && fileSaved.lastModified() == connection.getLastModified() && fileSaved.length() == getSize()){
                status = STATUS_DOWNLOAD.UPDATED;
                downloaded = contentLength;
                stateChanged();
            }else if(fileSaved.exists() && fileSaved.lastModified() != connection.getLastModified()){
                status = STATUS_DOWNLOAD.OUTDATED;
            }else if(fileSaved.exists() &&  fileSaved.length() != getSize() ){
                status = STATUS_DOWNLOAD.OUT_OF_SIZE;
            }
            else {
                status = STATUS_DOWNLOAD.NOT_DOWNLOADED;
            }
            logger.debug("{} foi verificado em: {} , status: {}", getFileName(), SIMPLE_DATE_FORMAT.format(new Date()), status.getDesc());
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            error();
        } finally {
            // Close file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }

            // Close connection to server.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }

    // Get this download's URL.
    public String getUrl() {
        return urlToDownload.toString();
    }
     
    // Get this download's size.
    public int getSize() {
        return size;
    }
     
    // Get this download's progress.
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }
     
    // Get this download's status.
    public STATUS_DOWNLOAD getStatus() {
        return status;
    }
     
    // Pause this download.
    public void pause() {
        start = false;
        status = STATUS_DOWNLOAD.PAUSED;
        stateChanged();
    }
     
    // Resume this download.
    public void resume() {
        status = STATUS_DOWNLOAD.DOWNLOADING;
        stateChanged();
        download();
    }
     
    // Cancel this download.
    public void cancel() {
        status = STATUS_DOWNLOAD.CANCELLED;
        stateChanged();
    }
     
    // Mark this download as having an error.
    private void error() {
        status = STATUS_DOWNLOAD.ERROR;
        logger.debug("Ocorreu um erro ao baixar o arquivo {} em {} ", getFileName(), SIMPLE_DATE_FORMAT.format(new Date()));
        logger.error("Ocorreu um erro ao baixar o arquivo {} em {} ", getFileName(), SIMPLE_DATE_FORMAT.format(new Date()));
        stateChanged();
    }
     
    // Start or resume downloading.
    public void download() {
        if(!start || status.equals(STATUS_DOWNLOAD.PAUSED)){
            start = true;
            Thread thread = new Thread(this);
            thread.start();
        }
    }
     
    // Get file name portion of URL.
    private String getFileName(URL url) {
        String fileName = url.getFile();
        String test = fileName.substring(fileName.lastIndexOf('/') + (fileName.contains("/lib/")?-4: 1));
        return fileName.substring(fileName.lastIndexOf('/') + (fileName.contains("/lib/")?-4: 1));
    }

    // Download file.
    public void run() {
        InputStream stream = null;
        fileSaved = new File(folderToSave + "/" + getFileName());
        File fileTmpSaved = new File(folderToSave + "/" + getFileName()+".part");
        long time = new Date().getTime();
        try {
            status = STATUS_DOWNLOAD.DOWNLOADING;
            stateChanged();
            downloaded = 0;
            // Open connection to URL.
            HttpURLConnection connection =
                    (HttpURLConnection) urlToDownload.openConnection();
            // Specify what portion of file to download.
            connection.setRequestProperty("Range",
                    "bytes=" + downloaded + "-");
             
            // Connect to server.
            connection.connect();

            time = connection.getLastModified();

            // Make sure response code is in the 200 range.
            if (connection.getResponseCode() / 100 != 2) {
                error();
            }
            // Check for valid content length.
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }
            if(fileSaved.exists() && fileSaved.lastModified() == connection.getLastModified() && fileSaved.length() == getSize()){
//                logger.debug("Atualizado");
                status = STATUS_DOWNLOAD.UPDATED;
                size = contentLength;
                downloaded = contentLength;
                stateChanged();
            }
            fileSaved.setLastModified(connection.getLastModified());

            if(!fileSaved.exists()){
                String s = FilenameUtils.getPrefix(fileSaved.getAbsolutePath())+ FilenameUtils.getPath(fileSaved.getAbsolutePath());
                Files.createDirectories( Paths.get(FilenameUtils.getPrefix(fileSaved.getAbsolutePath())+ FilenameUtils.getPath(fileSaved.getAbsolutePath())));
            }

            if(!fileTmpSaved.exists()){
                String s = FilenameUtils.getPrefix(fileTmpSaved.getAbsolutePath())+ FilenameUtils.getPath(fileTmpSaved.getAbsolutePath());
                Files.createDirectories( Paths.get(FilenameUtils.getPrefix(fileTmpSaved.getAbsolutePath())+ FilenameUtils.getPath(fileTmpSaved.getAbsolutePath())));
            }

             
      /* Set the size for this download if it
         hasn't been already set. */
            if (size == -1) {
                size = contentLength;
                stateChanged();
            }
             
            // Open file and seek to the end of it.
            file = new RandomAccessFile(fileTmpSaved, "rw");
            file.seek(downloaded);
            stream = connection.getInputStream();
            while (status == STATUS_DOWNLOAD.DOWNLOADING) {
        /* Size buffer according to how much of the
           file is left to download. */
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[size - downloaded];
                }
                 
                // Read from server into buffer.
                int read = stream.read(buffer);
                if (read == -1)
                    break;
                 
                // Write buffer to file.
                file.write(buffer, 0, read);
                downloaded += read;
                stateChanged();
            }
             
      /* Change status to complete if this point was
         reached because downloading has finished. */
            if (status == STATUS_DOWNLOAD.DOWNLOADING ) {
                status = STATUS_DOWNLOAD.COMPLETE;
                logger.debug("Arquivo:{}, Tam: {} foi baixado e atualizado com sucesso em {} ", getFileName()
                        , DownloadsTableModel.getStringSizeLengthFile(getSize())
                        , SIMPLE_DATE_FORMAT.format(new Date()));
                stateChanged();
            }

        } catch (Exception e) {
            e.printStackTrace();
            error();
        } finally {
            // Close file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }
             
            // Close connection to server.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
            if (status.equals(STATUS_DOWNLOAD.COMPLETE) || (status.equals(STATUS_DOWNLOAD.DOWNLOADING) && getSize() == getDownloaded())) {
                fileTmpSaved.setLastModified(time);
                try {
                    FileUtils.copyFile(fileTmpSaved, fileSaved);
                    fileSaved.setLastModified(time);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            stateChanged();
        }
        try {
            fileTmpSaved.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Notify observers that this download's status has changed.
    private void stateChanged() {
        setChanged();
        notifyObservers();
        if(getStatus().equals(STATUS_DOWNLOAD.COMPLETE) && getFileSaved().getName().contains(".zip")){
            try {
                new UnZip(getFolderToSave()).unzip(getFileSaved().getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(getStatus().equals(STATUS_DOWNLOAD.DOWNLOADING) && getSize() == getDownloaded()){
            status = STATUS_DOWNLOAD.COMPLETE;
            stateChanged();
        }
    }

    public static void removeAllFilesByFilter(File folderToSave, String filter){
        List<File> files = (List<File>) FileUtils.listFiles(folderToSave, new String[]{filter}, true);
        for (File file : files) {
            try {
                System.out.println("file: " + file.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}