package io.github.alejandrodorich.searchengine;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Utility class that provides helper methods for tests. 
 */
public class Utils {

    /**
     * Parse a JSON file and return the parsed object.
     * 
     * @param filename      Filename of the JSON file.
     * @return              Parsed JSON object. 
     * @throws IOException  If an I/O error occurs while reading the file.
     */
    public static JsonObject parseJSONFile(String filename) throws IOException {
        Path path = Paths.get(filename);
        String content = Files.readString(path);
        return new Gson().fromJson(content, JsonObject.class);
    }


    /**
     * Parse all JSON files in a given folder and return a list with all parsed objects.
     * 
     * @param folderName    Folder containing all JSON files.
     * @return              List containing all parsed JSON objects.
     * @throws IOException  If an I/O error occurs while reading the file.
     */
    public static List<JsonObject> parseAllJSONFiles(Optional<String> folderName) throws IOException {
        String fN = folderName.orElse("intranet");
        List<JsonObject> jsonObjects = new ArrayList<>();
        // Get all files in the folder
        File folder = new File(fN);
        File[] files = folder.listFiles();
        if (files == null) throw new IOException("Folder not found: " + fN);
        // Iterate over all files
        for (File file : files) {
            // Parse the JSON file
            jsonObjects.add(parseJSONFile(file.getAbsolutePath()));
        }
        return jsonObjects;
    }
}
