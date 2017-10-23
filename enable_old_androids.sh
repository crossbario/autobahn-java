#!/bin/sh

set -e

# Uncomment streamsupport gradle library
sed -i "s|//api 'net.sourceforge.streamsupport:streamsupport-cfuture:|api 'net.sourceforge.streamsupport:streamsupport-cfuture:|g" autobahn/build.gradle

# Change SDK support to Android 4.0.3
sed -i 's/minSdkVersion.*/minSdkVersion 15/g' autobahn/build.gradle
sed -i 's/minSdkVersion.*/minSdkVersion 15/g' demo-gallery/build.gradle

# Replace imports to streamsupport.
find -name *.java -print0 | xargs -0 sed -i 's/import static java.util.concurrent.CompletableFuture.runAsync;/import static java8.util.concurrent.CompletableFuture.runAsync;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.concurrent.CompletableFuture;/import java8.util.concurrent.CompletableFuture;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.concurrent.CompletionException;/import java8.util.concurrent.CompletionException;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.concurrent.ForkJoinPool;/import java8.util.concurrent.ForkJoinPool;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.function.BiConsumer;/import java8.util.function.BiConsumer;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.function.BiFunction;/import java8.util.function.BiFunction;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.function.Consumer;/import java8.util.function.Consumer;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.function.Function;/import java8.util.function.Function;/g'
find -name *.java -print0 | xargs -0 sed -i 's/import java.util.function.Supplier;/import java8.util.function.Supplier;/g'
