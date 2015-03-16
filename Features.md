# Hounder features #

### Designed for flexibility ###

  * Works as a complete solution (from crawling the web to providing a search interface).
  * Works as a complement to existing solutions (feeding a content system or indexing a data stream).
  * Crawl specific sites in depth or the web at large, searching for relevant pages and automatically classifying them
  * Add custom modules to indexer, crawler and searcher to add functionality.

### Installation ###

  * GUI and Command line installer.
  * Configuration wizard for most common uses.

### Integration ###

  * Searcher supports XML-RPC, RMI and OpenSearch.
  * Indexer supports XML-RPC and RMI.

### Indexing ###

  * Document processing pipeline defined by modules, defined as plugins: create and add your own.
  * Existent modules include: filtering spam, adding certain fields, logging, etc.
  * Manage when and how index updates are submitted to the searcher.

### Crawler ###

  * Bayesian filter to determine if a page is of interest or to which category it belongs.
  * Politeness.
  * Detects page content change and adapts frequency of recrawling.
  * Document processing pipeline defined by modules, defined as plugins: create and add your own.
  * Existent modules include: whitelisting, blacklisting, boosting, classifying, caching, indexing, etc.

### Search results ###

  * Snippet generation.
  * Results grouping.
  * Boosting.

### Queries ###

  * Define fields for your documents and search on the fields you want.
  * Operators: Or, And, Not.
  * Phrase recognition.

### Performance ###

  * Results caching.
  * Smart query execution and queue size management.

### Monitoring and controlling ###

  * Monitor and control all nodes of Hounder with the clustering web application.