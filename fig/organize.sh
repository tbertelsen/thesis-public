#!/bin/bash
mkdir CommonCount
mkdir Cosine
mkdir Edgar
mkdir Pearson
mkdir length
mkdir stats-alignment
mkdir stats-alignmentLog
mkdir stats-cosine
mkdir stats-cosineLog
mkdir stats-precision-recall
mkdir k-stats

# Must be first, to avoid conflict with other move commands
mv k_*.png k-stats/

mv *CommonCount.png CommonCount/
mv *Cosine.png Cosine/
mv *Edgar.png Edgar/
mv *Pearson.png Pearson/
mv *length.png length/

mv stats_*_alignment.png stats-alignment/
mv stats_*_alignmentLog.png stats-alignmentLog/
mv stats_*_cosine.png stats-cosine/
mv stats_*_cosineLog.png stats-cosineLog/
mv stats_*_precision-recall.png stats-precision-recall/
