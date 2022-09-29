
# deploy site (clover:instrument takes a long time)
mvn compile clover:instrument -P java-8 -Dmaven.compiler.useIncrementalCompilation=false
mvn install site site:stage site-deploy -Dmaven.compiler.useIncrementalCompilation=false
