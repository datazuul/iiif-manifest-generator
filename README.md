# datazuul app: IIIF Manifest Generator

A simple IIIF (<https://iiif.io>) manifest generator using [IIIF API library](https://github.com/dbmdz/iiif-apis).

## Usage

Put all images (of your book) into a directory. Directory name is used as manifest identifier. Image names (without extension) are used for image identifier.
Then call generator using the following options:

- Specify absolute directory path containing images using option `--d=...`.
- Specify your iiif server's image endpoint, e.g. `--imageEndpoint=http://www.yourdomain.com/iiif/image/2.1/`
- Specify your iiif server's presentation endpoint, e.g. `--presentationEndpoint=http://www.yourdomain.com/iiif/presentation/2.1/`

Only '.jpg'-images are detected for now.
Manifest-json will be generated to console. So redirect output to destination file.

Example:

```sh
$ java -jar datazuul-iiif-manifest-generator-1.0.0-SNAPSHOT.jar --d=/var/www/repository/books/my_book_id/ --imageEndpoint=http://localhost:8080/image/v2/ --presentationEndpoint=http://localhost:8080/presentation/v2/ > /var/www/repository/books/my_book_id/manifest-localhost.json
```

For serving manifest and images we recommend the highly configurable [Hymir IIIF Server](https://github.com/dbmdz/iiif-server-hymir).