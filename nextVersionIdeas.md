This place is for discussion, regardless of whether they will get implemented or not

# Problems of the current version #

  * It is not possible to know the fragmentation state of the index.
_Even though in general we want to have control of when the optimize is run, at this moment we don't have a way to know from the code if an optimization is needed. Sometimes it would be good to let the indexer take complete care of maintaining the index "reasonably optimized"._
  * Update method (delete + add) is not optimal
_Updating documents fragments the index too much. Furthermore, updating only a part of the document is a common task but it is difficult to gather the rest of the info to reindex it completely_
  * It is not possible to boost per word or phrase within a particular field
  * The query parser doesn't take the word order into account
_The de facto standard (Google) gives more importance to the words according to the order in which they are. Queries that match this behaviour could be done, but SloppyPhraseMatchQuery (or something) should be considered to see if it is a more viable alternative in terms of performance._

# New modules #
## New Writer module ##
### New contract ###

In contrast to the old Writer, this one would not have a defensive behavior, nor would it validate anything. The idea is to let this class be as clean as possible, and let all validations to other modules, probably the FormatValidatingModule. For the same reason, there would be no more optional fields.

### New format ###

```
<documentAdd>
    <documentId>String</documentId>
    <boost>Float</boost>
    <dynamicBoostPayload>HexEncodedByteArray</dynamicBoostPayload>
    [<storedField name=String>String</storedField>]*
    [<indexedField name=String>
        <token text=String start=int end=int boost=float
</documentAdd>
```

## FormatValidatingModule ##

This new module does the validation that the writer will stop doing. It basically checks that the documents respect the format required by the writer. The idea to take this functionality apart from the writer is based on two reasons:
  * The writer was a monolithic, complicated class. Assuming that the input is ok simplifies the class very much.
  * Even though most installations would use this module, there are cases with feeders and well debugged pipelines where this module can be omitted, increasing the performance.