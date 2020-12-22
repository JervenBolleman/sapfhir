# SapFhir

Join genomic variation graphs with public data or internal medical data e.g. FHIR.
by having a FAIR data access, using W3C sparql as a standard protocol.

# Status

This is a [RDF4j](https://rdf4j.org/) SAIL implementation that can take any handlegraph4j 
implementation and represent it as a [W3C sparql 1.1](https://www.w3.org/TR/sparql11-query/) endpoint. 

It is functionally complete. Performance depends hugly on the specific handlegraph implementation.

It is currently read-only, but could be made read/write.

There is a query optimizer that is active that can significantly rewrite queries for the best
performance.

# Example queries

```sparql
#Find the ten most forward to forward connected nodes (needs a lot of RAM)
PREFIX vg:<http://biohackathon.org/resource/vg#> 

SELECT ?node 
WHERE 
{
  ?node vg:linksForwardToForward ?node2 .
} 
GROUP BY ?node 
ORDER BY (COUNT(?node2)) 
LIMIT 10
```

```sparql
# Counts the number of sequences of length 1 in the graph
PREFIX vg:<http://biohackathon.org/resource/vg#>
SELECT 
  (COUNT(?n) AS ?c)
WHERE {
  ?n rdf:value ?sequence .
  FILTER(strlen(?sequence) ==1)
}
```

```sparql
# Counts the number of sequences with an R ambiguous nucleotide code
# handlegraph4j lower cases all dna sequences.
PREFIX vg:<http://biohackathon.org/resource/vg#>
SELECT 
  (COUNT(?n) AS ?c)
WHERE {
  ?n rdf:value ?sequence .
  FILTER(contains(?sequence, 'r'))
}
```

```sparql
# List all the Paths in the variation graph
PREFIX vg:<http://biohackathon.org/resource/vg#>
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>
SELECT 
  ?path 
  ?pathLabel
WHERE {
  ?path a vg:Path ;
        rdfs:label ?pathLabel .
}
```


