#!/bin/bash
wget $1 -O content.xml
echo -n "<documentId>$1</documentId>" >url.xml
cat testdata/header.xml content.xml testdata/middle.xml url.xml testdata/footer.xml >input.xml
rm content.xml
rm url.xml
java -cp lib/hounder-trunk.jar com.flaptor.hounder.test.TestClient localhost:9003 input.xml
