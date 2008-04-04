VERSION=1.0

ant distsrc distbin
scp product-output/hounder-trunk-binary_installer.tgz root@bonoki.com:/var/www/lighttpd/hounder.org/htdocs/downloads/hounder-${VERSION}-binary_installer.tgz
scp product-output/hounder-trunk-src.tgz root@bonoki.com:/var/www/lighttpd/hounder.org/htdocs/downloads/hounder-${VERSION}-binary_installer.tgz
