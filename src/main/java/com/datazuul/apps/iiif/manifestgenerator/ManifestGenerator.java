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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class ManifestGenerator {

  public String generateManifest(final String manifestIdentifier, final List<Path> files, String imageEndpoint, String presentationEndpoint)
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
    return json;
  }

  private static void addPage(String manifestIdentifier, Sequence sequence, int pageCounter, Path file, String imageEndpoint, String presentationEndpoint)
      throws IOException, URISyntaxException {
    System.err.println(file.toAbsolutePath());

    int width = -1;
    int height = -1;
    try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
      final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
      if (readers.hasNext()) {
        ImageReader reader = readers.next();
        try {
          reader.setInput(in);
          width = reader.getWidth(0);
          height = reader.getHeight(0);
        } finally {
          reader.dispose();
        }
      }
    }

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
