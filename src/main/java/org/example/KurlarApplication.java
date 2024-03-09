package org.example;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

public class KurlarApplication {

    private static final String NAME_OF_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";

    public static void main(String[] args) throws IOException {
        getCurrenciesFromUrl();
    }

    private static void getCurrenciesFromUrl() throws IOException {
        URL url = new URL(NAME_OF_URL);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        int connectionResponseCode = urlConnection.getResponseCode();
        long lastModified = urlConnection.getLastModified();

        checkLastModified(lastModified);
    }

    private static void checkLastModified(long lastModified) throws IOException {
        File lastModifiedFile = new File("lastModifiedFile.txt");
        FileWriter fileWriter = new FileWriter(lastModifiedFile);
        Scanner myReader = new Scanner(lastModifiedFile);
        String fileModifiedDate = null;

        if (lastModifiedFile.createNewFile()) {
            fileWriter.write(String.valueOf(lastModified));
            fileWriter.close();
        }else {
            while (myReader.hasNextLine()) {
                fileModifiedDate = myReader.nextLine();
            }
            if(!fileModifiedDate.equals(String.valueOf(lastModified))){

            }else {

            }
            myReader.close();
        }

    }

    private static boolean connectURL() throws IOException {
        URL url = new URL(KurlarApplication.NAME_OF_URL);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        int connectionResponseCode = urlConnection.getResponseCode();
        urlConnection.setRequestMethod("GET");

        return connectionResponseCode == HttpsURLConnection.HTTP_OK;
    }
}
