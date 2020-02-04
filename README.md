# datazuul app: IIIF Manifest Generator

A simple IIIF (<https://iiif.io>) manifest generator using [IIIF API library](https://github.com/dbmdz/iiif-apis).

## Installation

To support JPEG2000 install `libopenjp2`:

Debian:

```sh
$ sudo apt-get install libopenjp2-7
```

Clone git-repository and build manifest generator using Maven:

```sh
$ mvn clean install
```

Executable JAR is located in `target` subdirectory after build.

## Usage

Put all images (of your book) into a directory. Directory name is used as manifest identifier. Directory namae and Image names (without extension; combined with two underscores: e.g. `my_book_id__001`) are used for image identifier.
Then call generator using the following options:

- Specify absolute directory path containing images using option `--d=...`.
- Specify your iiif server's image endpoint, e.g. `--imageEndpoint=http://www.yourdomain.com/iiif/image/2.1/`
- Specify your iiif server's presentation endpoint, e.g. `--presentationEndpoint=http://www.yourdomain.com/iiif/presentation/2.1/`

Image files of type JPEG ('.jpg'), JPEG2000 ('.jp2') and TIFF ('.tif') are detected.
Manifest-json will be generated to console. So redirect output to destination file.

Example:

```sh
$ java -jar datazuul-iiif-manifest-generator-1.0.0-SNAPSHOT.jar --d=/var/www/repository/books/my_book_id/ --imageEndpoint=http://localhost:8080/image/v2/ --presentationEndpoint=http://localhost:8080/presentation/v2/ > /var/www/repository/books/my_book_id/manifest-localhost.json
```

For serving manifest and images we recommend the highly configurable [Hymir IIIF Server](https://github.com/dbmdz/iiif-server-hymir).