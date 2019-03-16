package com.datazuul.apps.iiif.manifestgenerator;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Implementing ApplicationRunner interface tells Spring Boot to automatically
 * call the run method AFTER the application context has been loaded.
 */
@SpringBootApplication
public class Application implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);

//    SpringApplication app = new SpringApplication(Application.class);
//    app.setBannerMode(Banner.Mode.OFF);
//    app.setLogStartupInfo(false);
//    app.run(args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    LOGGER.info("STARTING THE APPLICATION");

    if (args.containsOption("d")
        && args.containsOption("imageEndpoint")
        && args.containsOption("presentationEndpoint")) {
      String imageDirectoryPath = args.getOptionValues("d").get(0);
      String imageEndpoint = args.getOptionValues("imageEndpoint").get(0);
      String presentationEndpoint = args.getOptionValues("presentationEndpoint").get(0);

      Path imageDirectory = Paths.get(imageDirectoryPath);
      final List<Path> files = new ArrayList<>();
      try {
        Files.walkFileTree(imageDirectory, new SimpleFileVisitor<Path>() {
                         @Override
                         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                           if (!attrs.isDirectory()) {
                             // TODO there must be a more elegant solution for filtering jpeg files...
                             final String filename = file.getFileName().toString().toLowerCase();
                             if (filename.endsWith(".jp2") || filename.endsWith(".jpg") || filename.endsWith(".tif")) {
                               files.add(file);
                             }
                           }
                           return FileVisitResult.CONTINUE;
                         }
                       });
      } catch (IOException e) {
        e.printStackTrace();
      }
      Collections.sort(files, new Comparator() {
                     @Override
                     public int compare(Object fileOne, Object fileTwo) {
                       String filename1 = ((Path) fileOne).getFileName().toString();
                       String filename2 = ((Path) fileTwo).getFileName().toString();

                       try {
                         // numerical sorting
                         Integer number1 = Integer.parseInt(filename1.substring(0, filename1.lastIndexOf(".")));
                         Integer number2 = Integer.parseInt(filename2.substring(0, filename2.lastIndexOf(".")));
                         return number1.compareTo(number2);
                       } catch (NumberFormatException nfe) {
                         // alpha-numerical sorting
                         return filename1.compareToIgnoreCase(filename2);
                       }
                     }
                   });

      final String manifestIdentifier = imageDirectory.getFileName().toString();
      ManifestGenerator manifestGenerator = new ManifestGenerator();
      String manifestJson = manifestGenerator.generateManifest(manifestIdentifier, files, imageEndpoint, presentationEndpoint);
      System.out.println(manifestJson);
    } else {
      System.out.println("Specify absolute directory path containing images using option '--d=...'."
                         + "Specify your iiif server's image endpoint, e.g. '--imageEndpoint=http://www.yourdomain.com/iiif/image/2.1/'"
                         + "Specify your iiif server's presentation endpoint, e.g. '--presentationEndpoint=http://www.yourdomain.com/iiif/presentation/2.1/'"
                         + " Directory name is used as manifest identifier. Only '.jpg'-images are detected for now.");
    }
  }
}
