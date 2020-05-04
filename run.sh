#!/bin/bash
set -eux

clients="blocking nonblocking"
threads="2 4 8 32"
noJvmForks=1
noRealIterations=10
noWarmupIterations=5
minTimePerWarmupIterations=3
# give each iteration a bit of breathing space
timeoutPerIterationSeconds=15
outputFormatType=csv
failOnAnyBenchmarkError=true
configString="
-f $noJvmForks -i $noRealIterations -wi $noWarmupIterations
-w $minTimePerWarmupIterations -to $timeoutPerIterationSeconds
-rf $outputFormatType -foe $failOnAnyBenchmarkError"
for client in $clients ; do
    outputFile=${client}\.${outputFormatType}
    # Run only singlethreaded perftests
    java -jar -Dcz.fi.muni.pa036.client=$client target/benchmarks.jar \
                                                    $configString \
                                                    -rff $outputFile
done
