package com.datazuul.apps.iiif.manifestgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.enums.ViewingDirection;
import de.digitalcollections.iiif.model.enums.ViewingHint;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;
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

      final String manifestIdentifier = imageDirectory.getFileName().toString();
      generateManifest(manifestIdentifier, files, imageEndpoint, presentationEndpoint);
    } else {
      // automatically generate the help statement
      System.out.println("Specify absolute directory path containing images using option '--d=...'."
                         + "Specify your iiif server's image endpoint, e.g. '--imageEndpoint=http://www.yourdomain.com/iiif/image/2.1/'"
                         + "Specify your iiif server's presentation endpoint, e.g. '--presentationEndpoint=http://www.yourdomain.com/iiif/presentation/2.1/'"
                         + " Directory name is used as manifest identifier. Only '.jpg'-images are detected for now.");
    }
  }

  private static void generateManifest(final String manifestIdentifier, final List<Path> files, String imageEndpoint, String presentationEndpoint)
      throws JsonProcessingException, IOException, URISyntaxException {
    // Start Manifest
    String manifestLabel = "Manifest for " + manifestIdentifier;
    Manifest manifest = new Manifest(presentationEndpoint + manifestIdentifier + "/manifest", manifestLabel);

    Sequence sequence = new Sequence(presentationEndpoint + manifestIdentifier + "/sequence/normal", "Current page order");
    sequence.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
    sequence.addViewingHint(ViewingHint.PAGED);
    manifest.addSequence(sequence);

    int i = 0;
    for (Path file : files) {
      i = i + 1;
      addPage(manifestIdentifier, sequence, i, file, imageEndpoint, presentationEndpoint);
    }

    String json = generateJson(manifest);
    System.out.println(json);
  }

  private static void addPage(String manifestIdentifier, Sequence sequence, int pageCounter, Path file, String imageEndpoint, String presentationEndpoint)
      throws IOException, URISyntaxException {
    System.err.println(file.toAbsolutePath());

    BufferedImage bimg = ImageIO.read(file.toFile());
    int width = bimg.getWidth();
    int height = bimg.getHeight();

    // add a new page
    Canvas canvas = new Canvas(presentationEndpoint + manifestIdentifier + "/canvas/canvas-" + pageCounter, "p. " + pageCounter);
    canvas.setWidth(width);
    canvas.setHeight(height);

    String filename = file.getFileName().toFile().getName();
    String filenameWithoutExtension = filename.substring(0, filename.lastIndexOf("."));
    String imageIdentifier = manifestIdentifier + "__" + filenameWithoutExtension;

    ImageContent img = new ImageContent(imageEndpoint + imageIdentifier + "/full/full/0/default.jpg");
    img.addService(new ImageService(imageEndpoint + imageIdentifier, ImageApiProfile.LEVEL_TWO));
    img.setWidth(width);
    img.setHeight(height);
    canvas.addImage(img);

    sequence.addCanvas(canvas);
  }

  public static String generateJson(Manifest manifest) throws JsonProcessingException {
    ObjectMapper mapper = new IiifObjectMapper();
    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest);
    return json;
  }

}
