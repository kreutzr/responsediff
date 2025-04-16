# -----------------------------------------
# Build the fat jar
# -----------------------------------------
# mvn clean package -U
mvn clean package

# -----------------------------------------
# Invoke as follows (Tested for Java 11)
#
# Json parameters:
# ================
# rootPath            (string) (should end with "/")
# xmlFilePath         (string) (rootPath will be put in front if present)
# xsltFilePath        (string)
# reportFileEnding    (string)
# storeResultPath     (string) (rootPath will be put in front if present)
# ticketServiceUrl    (string)
# candidateServiceUrl (string)
# candidateHeaders    (List of entries with two attributes "name" and "value".) This is required for e.g. passing server individual authentication headers)
# referenceServiceUrl (string)
# referenceHeaders    (List of entries with two attributes "name" and "value".) This is required for e.g. passing server individual authentication headers)
# referenceFilePath   (string)
# controlServiceUrl   (string)
# controlHeaders      (List of entries with two attributes "name" and "value".) This is required for e.g. passing server individual authentication headers)
# responseTimeoutMs   (long)
# epsilon             (double)
# exitWithExitCode    (boolean)
#
# -----------------------------------------
java -cp target/responsediff-0.0.1-SNAPSHOT.jar com.github.kreutzr.responsediff.ResponseDiff '{ "rootPath" : "/c:/home/rkreutz/work/develop/test/responsediff/", "xmlFilePath" : "src/test/resources/com/github/kreutzr/responsediff/test_legacy/responseDiffSetup.xml", "storeResultPath" : "../test-results/", "candidateHeaders" : [ { "name" : "aaa", "value" : "bbb" } ], "referenceHeaders" : [ { "name" : "ccc", "value" : "ddd" } ], "controlHeaders" : [ { "name" : "eee", "value" : "fff" } ] }'
