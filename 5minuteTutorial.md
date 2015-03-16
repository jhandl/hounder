[![](http://hounder.org/images/hounder2.png)](http://hounder.org)

# Five-minute tutorial #

**Welcome to the Hounder tutorial, the fastest and easiest way to create a vertical search engine!**

You have downloaded Hounder, so what's next?

How about a vertical search on the content of [wikipedia](http://www.wikipedia.org)? We will start by crawling the entire site and then we will refine the configuration to see how we can crawl only pages in a specific language.

## Installing Hounder ##

Go to the installer directory and execute the installer:

```
./install-graphic-mode.sh
```

You can also use the command-line, text-only installer:

```
./install.sh
```

You will get a welcome screen, **click next**

You will be prompted to select the installation type. Since our aim is to keep this tutorial simple as possible, we will install all Hounder components on your machine.

So select  **“install all components in this machine”** and **click next**

This will install all of the following components:
  * searcher
  * indexer
  * crawler
  * cache server
  * clustering webapp

A confirmation window appears, **click next**

A window prompting for the Cache Server configuration (which is the server for cached versions of documents) appears.
You have to enter an external name so that the cache version of a document can be retrieved by the user browser (localhost will work only from your machine). So, **enter your host name** and **click next**.

Now you will be asked to enter the base port and base installation directory. Hounder uses a range of ports for its various servers, starting from the base port. Do not change this number unless those ports are already taken. **Set the base port and dir** (or leave the defaults) to where you want to install Hounder and **click Next**. The installer will copy the files to the destination directory.

Once the installer is finished, it will open the crawler configuration wizard to help you configure the crawler (note that you have two open windows now, it's OK).

## Configuring the crawler ##

_The crawler can be configured to do many different things, and the configuration options are vast. This wizard is only intended to set the most common options._

A welcome window is opened, **click next**

The crawler constantly fetches known pages, scan them for links and uses those links to know which pages to collect during the next phase. The initial page(s) where all the crawling starts, is(are) called the seed. We will use only one page as a seed, and will enter it manually, so **Select "Enter seed URLs manually"** and **click next**.

Hounder asks you for some seed pages.  Initially, you will need to supply at least one page for the crawler to start its work. For this tutorial, we will set http://www.wikipedia.org as the only seed.

In the 'Enter seed URLs' window, **delete every line** and **add the following**
```
 http://www.wikipedia.org/
```
then **click next**.

Now, you need to select the hotspots regular expression source. As in previous step, we will do it manually. So **select "Enter hotspots regular expressions manually** and **click next**.

You probably are asking What is the hotspots regular expression? Well... While extracting links from the collected pages, the crawler will find many useful links as well as several that will not be useful for this particular crawl. For example, most wikipedia articles have a section called “external links” that point to sites other than wikipedia. In Hounder terminology, a page that has to be crawled is known as “hotspot”. And you can tell Hounder which pages it should fetch using the hotspot regular expression.

The format of the lines of this file is 

&lt;prefix&gt;

 | 

&lt;regex&gt;

 || 

&lt;tags&gt;

. Tags is an advanced feature, so we just ignore it here. Only pages that match will be collected by the crawler.

> To focus the crawler to wikipedia's pages only, in the 'Enter hotspot regex' window, **delete every line** and **add the following**
```
http:// | .*wikipedia\.org/.*
```
then **click next**.

The basic configuration is now complete. **Click finish** to exit the configuration.Then **click next** on the Hounder Installation Wizard.

Congratulations!!!  The installation is now complete. Note the path where the components were installed, This is called the "base dir". Remember it as you will need it soon. **Click finish** to exit the installation.

There is one last thing left to do: make sure the indexer can send an index to the searcher using an ssh connection that doesn't need user interaction. To test it, just run "ssh localhost". If you are being asked for a password, you need to create a pair of keys (http://www.ece.uci.edu/~chou/ssh-key.html). The first time ssh connects to a machine it will ask you to authenticate it: just type yes.

## Running Hounder ##

It's time to see it in action. Go to the installation directory (base dir) and start the services by entering the following:

```
cd <installation-dir>
./start-all.sh
```

To check that all services are running, execute
```
./status.sh
```

It will take some time for the crawler to get enough results to make the search interesting, so let's take a peek at the crawler progress in the meantime. The clustering webapp lets you check the state of all components of Hounder at once, showing logs and status. You can access it using this URL in your browser (assuming "base port" is 47000):

```
http://localhost:47050
```

You should see now four components: the searcher, indexer, crawler and cache server, each one associated to a status. This page will show you if something is wrong.

Click on the state of crawler to get to the crawler node page. There you can access the crawler logs, so open the ERR log which will show you all the fetched pages in lines like this:

```
071121 204227 fetching http://so.wikipedia.org/wiki/Bogga_Hore
071121 204230 fetching http://kw.wikipedia.org/wiki/Pennfolenn/Penfolen
071121 204232 fetching http://bar.wikipedia.org/wiki/Hauptseitn
```

We see that this is collecting pages in strange languages. If we want to fetch only English pages we will need to adjust the crawler's hotspots. Let's do it!

Stop the crawler:

```
cd <installation-directory>/crawler
./stop.sh
```

Now we can use the crawler configuration wizard again by executing:

```
./configure-graphic-mode.sh 
```

Or we can edit the hotspots.regex file in the crawler's configuration directory (

<installation\_dir>

/crawler/conf/hostpots.regex)

English wikipedia's pages can be matched with the following hotspots line:

```
http://en.wikipedia.org/ | .* || 
```

By this point, the clustering webapp should show you that the crawler component is unreachable.  You can recheck the component by refreshing the clustering page.

So, start the crawler again (./start.sh) and go see that the log now contains English pages.

```
071121 205939 fetching http://en.wikipedia.org/wiki/1950
071121 205939 fetching http://en.wikipedia.org/wiki/Ceasefire
071121 205939 fetching http://en.wikipedia.org/wiki/Japanologist
071121 205939 fetching http://en.wikipedia.org/wiki/Wikipedia:Selected_anniversaries/November
071121 205939 fetching http://en.wikipedia.org/wiki/Talk:Main_Page
071121 205943 fetching http://en.wikipedia.org/wiki/Her_Majesty%27s_Revenue_and_Customs
071121 205944 fetching http://en.wikipedia.org/wiki/Henry_III_of_England
071121 205950 fetching http://en.wikipedia.org/wiki/Oil_reservoir
071121 205953 fetching http://en.wikipedia.org/wiki/World_Trade_Center_site
```

## It's time to search! ##

You can execute searches from your browser using the built in searching webapp. To do this, simply use the following URL in the browser (assuming base port was set to 47000):

```
http://localhost:47012/websearch
```

Type in a query. In the results you will see the URL, a snippet of the content, and a link to the cached version. Try it and see for yourself!

_Note that the searcher index takes approximately 5 minutes to be updated, so you will have to let a few minutes pass before you begin to see your results._

**That's it!  You have created your first vertical search engine with Hounder!**