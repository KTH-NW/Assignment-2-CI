package ci;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.ServerSocket;

public class FileIOTest {

    @Test
    @DisplayName("FileIO::writeToFile::throws IOException when it can't find the file")
    public void testWriteToFileIOException() {
        assertThrows(IOException.class, () -> {
            FileIO.writeToFile("", "", "");
        });
    }

    @Test
    @DisplayName("FileIO::createFolder::throws InvalidObjectException with bad input")
    public void testCreateFolderInvalidObjException() {
        assertThrows(InvalidObjectException.class, () -> {
            FileIO.createFolder("");
        });
    }

    @Test
    @DisplayName("FileIO::createFolder::Works as expected")
    public void testCreateFolder() {
        try {
            FileIO.createFolder("./testfolder");
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("FileIO::deleteFolder::Works as expected")
    public void testDeleteFolder() {
        try {
            FileIO.deleteFolder("./testfolder");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("FileIO::deleteFolder::Throws IOException when called on non-existent file ")
    public void testDeleteFolderOnNothing() {
        assertThrows(IOException.class, () -> {
            FileIO.deleteFolder("");
        });
    }

    @Test
    @DisplayName("FileIO::listFileNames::Throws NullPointerException when called on nothing and returns null")
    public void testListFileNamesinNonExistentFolder() {
        assertThrows(NullPointerException.class, () -> {
            FileIO.listFileNames(null);
        });
        assertEquals(null, FileIO.listFileNames(""));
    }

    @Test
    @DisplayName("FileIO::listFileNames::Works as intended")
    public void testListFileNames() {
        try {
            FileIO.createFolder("testfolder1");
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }
        try {
            FileIO.writeToFile("/test0", "test", "testfolder1");
            FileIO.writeToFile("/test1", "test", "testfolder1");
            FileIO.writeToFile("/test3", "test", "testfolder1");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(3, (FileIO.listFileNames("testfolder1")).length);
        try {
            FileIO.deleteFolder("testfolder1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("FileIO::contentOfFile::Throws appropriate exception")
    public void testContentOfFileExceptions() {
        assertThrows(FileNotFoundException.class, () -> FileIO.contentOfFile(new File("")));
    }

    @Test
    @DisplayName("FileIO::contentOfFile::Works as intended")
    public void testContentOfFile() {
        try {
            FileIO.writeToFile("test", "test", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            assertEquals("test", FileIO.contentOfFile(new File("test")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileIO.deleteFolder("test");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
