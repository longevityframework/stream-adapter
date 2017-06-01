# A collection of converters between various Scala streaming libraries

Did you ever find yourself with a stream from one streaming library, but some other part of your
program expects a stream from some other library? Do you want to provide streaming as part of your
API, don't want to limit yourself to a single streaming library? `stream-adapter` provides tooling
to help address both of these situations.

## Converting between two streaming libraries

`stream-adapter` provides conversions between the following Scala streaming libraries:

- [Akka Streams](http://doc.akka.io/docs/akka/2.4.17/scala/stream/index.html)
- [FS2](https://github.com/functional-streams-for-scala/fs2)
- [iteratee.io](https://github.com/travisbrown/iteratee)
- [Play enumerators](https://www.playframework.com/documentation/2.5.x/Enumerators)

More can be added relatively easily, as we will see below. Let's start with an Akka `Source`:

```scala
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
implicit val actorSystem = ActorSystem("streamadapter")
implicit val materializer = ActorMaterializer()
val akkaSource = Source(0.until(10))
```

We can convert this to an `fs2.Stream` like so:

```scala
val fs2Stream: fs2.Stream[fs2.Task, Int] = {
  import streamadapter._
  import streamadapter.akka._
  import streamadapter.fs2._
  adapt[AkkaSource, FS2Stream, Int](akkaSource)
}
```

Converting streams requires a small handful of wildcard imports. I like to put these in an anonymous
block, as above, so they don't apply to the rest of the code in the file.

Unfortunately, the effects for `fs2.Stream` is hardwired to `fs2.Task` for the moment. We should be
able to handle other effects in the future.

Let's now convert that FS2 stream into an `io.iteratee.Enumerator`:

```scala
val iterateeIoEnumerator: io.iteratee.Enumerator[cats.Eval, Int] = {
  implicit val S = fs2.Strategy.fromFixedDaemonPool(8, threadName = "worker")
  import streamadapter._
  import streamadapter.fs2._
  import streamadapter.iterateeio._
  adapt[FS2Stream, EvalEnumerator, Int](fs2Stream)
}
```

All three of the type arguments to `adapt` should be inferrable by the compiler. The first and the
third argument are inferrable from the argument, `fs2Stream`, and the second argument should be
inferrable from the left-hand side. But I haven't figured out how to get the compiler to infer any
of them yet. Can you help? It's not as simple as you might think, because the implicit resolution
has to navigate between types with two type parameters, such as `Enumerator[Eval, Int]`, and types
with a single type parameter, such as `streamadapter.iterateeio.EvalEnumerator[Int]`.

Let's in turn convert this iteratee.io enumerator into a Play enumerator:

```scala
val playEnumerator: play.api.libs.iteratee.Enumerator[Int] = {
  import streamadapter._
  import streamadapter.iterateeio._
  import streamadapter.play._
  import scala.concurrent.ExecutionContext.Implicits.global
  adapt[EvalEnumerator, PlayEnumerator, Int](iterateeIoEnumerator)
}
```

All four enumerators above will produce the same elements, zero through nine, as you can see for
yourself by running [Usage.scala](https://github.com/longevityframework/stream-adapter/blob/master/core/src/test/scala/streamadapter/usage/Usage.scala).

## How it works

The `stream-adapter` provides a class `Chunkerator` that captures most of the functionality of a
stream in a synchronous data structure. Streams are re-runnable, so we provide a `Function0` that
produces an iterator:

```scala
trait Chunkerator[+A] extends Function0[CloseableChunkIter[A]] { // ...
```

The `CloseableChunkIter` is an iterator over "chunks", which are used by various streaming libraries
for performance reasons:

```scala
trait CloseableChunkIter[+A] extends CloseableIter[Seq[A]]
```

A `CloseableIter` is just an iterator that supports a `close` operation, so the consumer can
communicate to the producer that it finished early, allowing the producer to free up resources:

```scala
trait CloseableIter[+A] extends Iterator[A] {
  def close: Unit
}
```

For each streaming library, we provide two adapters: one from a `Chunkerator` to a stream, and the
other from a stream to a `Chunkerator`. We can then combine two adapters to produce an adapter
between any two streaming libraries, using `Chunkerator` as a mediator. This way, we can have eight
converters - two for each of four streaming libraries - instead of the twelve we would need if we
were going to provide custom adapters for every pair of streaming libraries. And if we add another
streaming library to the mix, we only need to add two adapters, instead of eight.

One advantage to this approach is for people who are writing libraries that provide a streaming API,
but don't want to lock it down to a single streaming library. In this case, they can just produce a
single `Chunkerator`, and use `stream-adapter` to produce streams from multiple libraries. In fact,
this is how I am using it. I only use the stream to `Chunkerator` converters for testing.

Of course, this approach does not rule out the possibility of providing custom adapters that remove
the mediating step. We may have to juggle the implicits a bit to make this work, but it shouldn't be
a big deal.

## Limitations

This early release is very raw, and there are a number of improvements that could be made. I've
created GitHub issues to keep track of all the ideas I've come up with. Rather than repeating myself
here, I'll ask you to browse the issues yourself:

- [https://github.com/longevityframework/stream-adapter/issues](https://github.com/longevityframework/stream-adapter/issues)

## Usage

We provide artifacts for Scala 2.11 and 2.12. We don't have 2.10 artifacts because there is not a
full suite of 2.10 artifacts for the four streaming libraries.

```scala
libraryDependencies += "org.longevityframework" %% "streamadapter" % "0.1.0"
```

All the underlying streaming libraries are included here as optional dependencies, so you will need
to bring in the libraries you want in your own project. Examples:

```scala
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.2"

libraryDependencies += "co.fs2" %% "fs2-core" % "0.9.6"

libraryDependencies += "org.typelevel" %% "cats" % "0.9.0"
libraryDependencies += "io.iteratee" %% "iteratee-core" % "0.12.0"

libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1"
```

## License

It's [Apache 2](http://www.apache.org/licenses/). I don't really have any reasons to pick a
different license than this seemingly de-facto open source license. If you have some good reasons
why this project should be released under a different license, please let me know.
