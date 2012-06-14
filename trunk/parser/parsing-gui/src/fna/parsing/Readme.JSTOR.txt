FNATaxonNameFinalizerStep1.java: continue markup name elements
FNATaxonNameFinalizerStep2.java: fix ranks for synonyms


FNANameReviewer.java: print out complex content of name related elements.


FNAElementReviewer.java: print out simple content of a given element.

FNAElevation.java: fix cases where habitats are included in elevation, like ... <phenology_fruiting>nov</phenology_fruiting><elevation>emergent shorelines; 0–1500 m;</elevation>

FNAGlobalDistribution.java: some global distributions should be ca or us_distribution. 

FNAHabitat2Fruiting.java:  used to fix cases like:
 *  <habitat>fruiting late spring–early summer (jul–sep). marshes</habitat>
    <habitat>streamsides</habitat>
    <habitat>ditches</habitat>
 =>
     <phenology_fruiting>fruiting late spring–early summer (jul–sep).</phenology_fruiting> <habitat>marshes</habitat>
    <habitat>streamsides</habitat>
    <habitat>ditches</habitat>


