package agent;

import commons.Machine;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import commons.exceptions.OSSUSNoAPIConnectionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Zipper {

    static void zipDir(
            final String zipFileName,
            final String dir,
            final Machine machine
    ) throws OSSUSNoAPIConnectionException {
        Zipper.createZipFromDirectory(dir, zipFileName, machine, true);
    }

    public static boolean createZipFromDirectory(
            final String directory,
            final String filename,
            final Machine machine,
            final boolean absolute
    ) throws OSSUSNoAPIConnectionException {

        File rootDir = new File(directory);
        File saveFile = new File(filename);

        ZipArchiveOutputStream zaos;

        try {
            zaos = new ZipArchiveOutputStream(new FileOutputStream(saveFile));
        } catch (FileNotFoundException e) {
            machine.logErrorMessage(e.getMessage());
            return false;
        }
        try {
            recurseFiles(rootDir, rootDir, zaos, absolute);
        } catch (IOException e2) {
            machine.logErrorMessage(e2.getMessage());

            try {
                zaos.close();
            } catch (IOException e) {
                machine.logErrorMessage(e.getMessage());
            }
            return false;
        }
        try {
            zaos.finish();
        } catch (IOException e1) {
            machine.logErrorMessage(e1.getMessage());
            return false;
        }
        try {
            zaos.flush();
        } catch (IOException e) {
            machine.logErrorMessage(e.getMessage());
            return false;
        }
        try {
            zaos.close();
        } catch (IOException e) {
            machine.logErrorMessage(e.getMessage());
            return false;
        }

        return true;
    }

    private static void recurseFiles(
            final File root,
            final File file,
            final ZipArchiveOutputStream zaos,
            final boolean absolute
    ) throws IOException {

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File file2 : files) {
                    recurseFiles(root, file2, zaos, absolute);
                }
            }
        } else if (!file.getName().endsWith(".zip") && !file.getName().endsWith(".ZIP")) {
            String filename;
            if (absolute) {
                filename = file.getAbsolutePath().substring(root.getAbsolutePath().length());
            } else {
                filename = file.getName();
            }
            ZipArchiveEntry zae = new ZipArchiveEntry(filename);
            zae.setSize(file.length());
            zaos.putArchiveEntry(zae);
            FileInputStream fis = new FileInputStream(file);
            IOUtils.copy(fis, zaos);
            fis.close();
            zaos.closeArchiveEntry();
        }
    }
}
