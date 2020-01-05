package com.zy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

public class GenerateTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(writeClasspathToFile("zzz"));
        System.out.println(Arrays.toString(args));
        Thread.sleep(60000);
    }

    private static String writeClasspathToFile(String classpath) {

        try {
            File file = File.createTempFile("EvoSuite_classpathFile",".txt");
            file.deleteOnExit();

            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            String line = classpath;
            out.write(line);
            out.newLine();
            out.close();

            return file.getAbsolutePath();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to create tmp file for classpath specification: "+e.getMessage());
        }

    }
}
