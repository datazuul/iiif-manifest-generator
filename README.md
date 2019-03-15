# datazuul app: IIIF Manifest Generator

A simple IIIF (<https://iiif.io>) manifest generator using [IIIF API library](https://github.com/dbmdz/iiif-apis).

## Usage

Put all images (of your book) into a directory. Directory name is used as identifier.
Then call generator using option "d" to specify absolute path to directory containing jpg-images:

```sh
$ java -jar datazuul-iiif-manifest-generator-1.0.0-SNAPSHOT.jar --d=/var/www/repository/books/my_book_id/ > manifest.json
```

Manifest-json will be generated to console. So redirect output to destination file.
