package gui;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DictionaryStorage {
    private static final String STORAGE_FILE = "dictionaries.conf";

    // Сохраняет список путей к словарям в файл (по одному пути на строку)
    public void saveDictionaries(List<String> dictionaryPaths) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STORAGE_FILE))) {
            for (String path : dictionaryPaths) {
                writer.write(path);
                writer.newLine();
            }
        }
    }

    // Загружает список путей из файла
    public List<String> loadDictionaries() throws IOException {
        List<String> paths = new ArrayList<>();
        File file = new File(STORAGE_FILE);
        if (file.exists() && file.length() > 0) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    paths.add(line.trim());
                }
            }
        }
        return paths;
    }
}
