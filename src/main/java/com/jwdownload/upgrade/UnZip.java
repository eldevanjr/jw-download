package com.jwdownload.upgrade;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnZip {
 
    private String output  = System.getProperty("java.io.tmpdir")+"/";
 
    public void unzip(String file) throws FileNotFoundException, IOException, ArchiveException {
 
        File inputFile = new File(file);
 
        InputStream is = new FileInputStream(inputFile);
        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
        ZipEntry entry = null;
 
        while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
 
            if (entry.getName().endsWith("/")) {
                File dir = new File(output + File.separator + entry.getName());
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                continue;
            }
 
            File outFile = new File(output + File.separator + entry.getName());
 
            if (outFile.isDirectory()) {
                continue;
            }

            FileOutputStream out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = ais.read(buffer)) > 0) {
                out.write(buffer, 0, length);
                out.flush();
            }
 
        }
    }

    public void unjar(String file, String fileToExtract) throws FileNotFoundException, IOException, ArchiveException {
        File inputFile = new File(file);
        InputStream is = new FileInputStream(inputFile);
        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("jar", is);
        ZipArchiveEntry entry = null;
        while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
            if(entry.getName().contains(fileToExtract)){
                if (entry.getName().endsWith("/")) {
                    File dir = new File(output + File.separator + entry.getName());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    continue;
                }
                File outFile = new File(output + File.separator + entry.getName());
                if (outFile.isDirectory()) {
                    continue;
                }
                FileOutputStream out = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int length = 0;
                while ((length = ais.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                    out.flush();
                }
            }
        }
    }

    /**
     * Read all the bytes for the current entry from the input to the output.
     */
    private void copyStream(InputStream in, OutputStream out, JarEntry entry)
            throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n = 0;
        long size = entry.getSize();
        while (-1 != (n = in.read(buffer)) && count < size) {
            out.write(buffer, 0, n);
            count += n;
        }
    }

    public static void unjarFolder(String jarFile, String filterToExtract, String folderToExtract) throws IOException, ArchiveException {
        File destFolder = new File(folderToExtract);
        if(!destFolder.exists()){
            destFolder.mkdirs();
        }
        File inputFile = new File(jarFile);
        InputStream is = new FileInputStream(inputFile);
        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("jar", is);
        ZipArchiveEntry entry = null;
        while ((entry = (ZipArchiveEntry) ais.getNextEntry()) != null) {
            if(entry.getName().contains(filterToExtract)){
                if (entry.getName().endsWith("/")) {
                    File dir = new File(folderToExtract + File.separator + entry.getName());
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    continue;
                }
                File outFile = new File(folderToExtract + File.separator + new File(entry.getName()).getName());
                if (!outFile.exists()) {
                    outFile.createNewFile();
                    FileOutputStream out = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int length = 0;
                    while ((length = ais.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                        out.flush();
                    }
                }
            }
        }
    }

}