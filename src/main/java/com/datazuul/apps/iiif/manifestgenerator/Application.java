package com.datazuul.apps.iiif.manifestgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
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
import javax.imageio.ImageIO;
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

    if (args.containsOption("d")) {
      String imageDirectoryPath = args.getOptionValues("d").get(0);
      Path imageDirectory = Paths.get(imageDirectoryPath);
      final List<Path> files = new ArrayList<>();
      try {
        Files.walkFileTree(imageDirectory, new SimpleFileVisitor<Path>() {
                         @Override
                         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                           if (!attrs.isDirectory()) {
                             // TODO there must be a more elegant solution for filtering jpeg files...
                             if (file.getFileName().toString().endsWith("jpg")) {
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

      generateManifest(imageDirectory.getFileName().toString(), files);
    } else {
      // automatically generate the help statement
      System.out.println("Specify absolute directory path containing images using option '--d=...'. Directory name is used as identifier. Only '.jpg'-images are detected for now.");
    }
  }

  private static void generateManifest(final String imageDirectoryName, final List<Path> files)
      throws JsonProcessingException, IOException, URISyntaxException {
    // Start Manifest
    String urlPrefix = "http://www.yourdomain.com/iiif/presentation/2.1/";
    String manifestLabel = "Manifest for " + imageDirectoryName;
    Manifest manifest = new Manifest(urlPrefix + imageDirectoryName + "/manifest", manifestLabel);

    List<Sequence> sequences = new ArrayList<>();
    manifest.setSequences(sequences);

    Sequence seq1 = new Sequence(urlPrefix + imageDirectoryName + "/sequence/normal", "Current page order");
    sequences.add(seq1);

    List<Canvas> canvases = new ArrayList<>();
    seq1.setCanvases(canvases);

    int i = 0;
    for (Path file : files) {
      i = i + 1;
      addPage(urlPrefix, imageDirectoryName, canvases, i, file);
    }

    String json = generateJson(manifest);
    System.out.println(json);
  }

  private static void addPage(String urlPrefix, String imageDirectoryName, List<Canvas> canvases, int pageCounter, Path file)
      throws IOException, URISyntaxException {
    System.out.println(file.toAbsolutePath());

    BufferedImage bimg = ImageIO.read(file.toFile());
    int width = bimg.getWidth();
    int height = bimg.getHeight();

    // add a new page
    Canvas canvas1 = new Canvas(urlPrefix + imageDirectoryName + "/canvas/canvas-" + pageCounter, "p-" + pageCounter);
    canvas1.setHeight(height);
    canvas1.setWidth(width);
    canvases.add(canvas1);

//    List<Image> images = new ArrayList<>();
//    canvas1.setImages(images);
//
//    Image image1 = new ImageImpl();
//    image1.setOn(canvas1.getId());
//    images.add(image1);
//
//    ImageResource imageResource1 = new ImageResourceImpl(urlPrefix + imageDirectoryName + "/" + fileName.toString());
//    imageResource1.setHeight(height);
//    imageResource1.setWidth(width);
//    image1.setResource(imageResource1);
//
//    Service service1 = new Service(urlPrefix + imageDirectoryName + "/" + fileName.toString() + "?");
//    service1.setContext("http://iiif.io/api/image/2/context.json");
//    service1.setProfile("http://iiif.io/api/image/2/level1.json");
//    imageResource1.setService(service1);
  }

  public static String generateJson(Manifest manifest) throws JsonProcessingException {
    ObjectMapper mapper = new IiifObjectMapper();
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest);
    return json;
  }

}
