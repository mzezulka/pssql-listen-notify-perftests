#!/bin/bash
set -eux

clients=blocking
threads="2 4 8 32"

mvn clean install


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
                                                    -rff $outputFile \
                                                    -e "perftests\.MultithreadedPerftests\.*"
    for noThreads in $threads ; do
        # redefine file name
        outputFile=${client}\-multithreaded-${noThreads}threads\.$outputFormatType
        java -jar -Dcz.fi.muni.pa036.client=$client target/benchmarks.jar \
                                                        $configString \
                                                        -t $noThreads \
                                                        -rff $outputFile \
                                                        -e "perftests\.Perftests\.*"
    done
done
