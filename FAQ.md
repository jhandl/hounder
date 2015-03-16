## Hounder's frequently asked questions ##

### How did Hounder get started? ###
Work on the project (formely known as Search4j) started in 2005 as an ad-hoc solution to gather information about torrent files and make it searcheable. During 2006 that solution was generalized and saw its first use as a web search engine. Later in 2006 the need to have a scalable solution was obvious and the project underwent a major refactoring process in order to support clusterization. During 2007 the adaptability of the code was put to a test with a variety of deployment scenarios ranging from a 30+ machine installation serving over 200 M documents, to small vertical search engines running on a single machine.

### How does Hounder compare to Lucene? ###
Lucene is a full text search library, intended to be a basic building block for applications needing text search functionality. Hounder, while it uses Lucene, is a search engine, providing all the required components to gather and find information with minimum extra coding, and often, no coding at all.

### How does Hounder compare to Nutch? ###
Hounder started using many parts of Nutch for its crawler, but many of them proved too slow and rigid for our purposes, so they were slowly replaced. The only remaining part of Nutch still in use is the Fetcher, because of its plugin architecture that understands the most common document formats found online and its flexible thread configuration that adapts well to different throughput scenarios.

### Is Hounder still active? ###
No, developement in Hounder stopped as Flaptor became a start-up and focused on IndexTank, a cloud-based search engine, which was acquired by LinkedIn in October 2011.

### What are the pre-requisites for running Hounder? ###
Hounder was designed to run on Linux platforms with Sun's Java 1.6 jvm.