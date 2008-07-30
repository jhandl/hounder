VERSION=1.0.1

rm -fr product-output
ant distsrc distbin
cd product-output
mv hounder-trunk-binary_installer.tgz hounder-${VERSION}-binary_installer.tgz
mv hounder-trunk-src.tgz hounder-${VERSION}-src.tgz
cd ..
scp product-output/hounder-*.tgz root@bonoki.com:/var/www/lighttpd/hounder.org/htdocs/downloads/
