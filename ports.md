# Hounder RPC Ports #
## Base-port + Offset ##

Hounder works with a base-port + offset, which means that it uses a range of ports starting from a base port.

The offsets and base port are defined in the common.properties file, and by default it is 47000

## Offsets ##

### Indexer ###
  * RMI = 0
  * XML-RPC = +1

### Searcher ###
  * RMI = +10
  * XML-RPC = +11
  * web & opensearch = +12 (http://host:47012/websearch & http://host:47012/opensearch)
  * post new index = +40

### Crawler ###
  * RMI = +20

### Cache Server ###
  * http = +30

### Clustering ###
  * searcher RPC = +41
  * indexer RPC = +42
  * crawler RPC = +43
  * cacheServer RPC = +44

  * web server = +50

### Log Analysis ###

  * web server = + 60

### Page Catcher ###

  * RMI = + 70

### Learning Webapp ###

  * web server = + 80