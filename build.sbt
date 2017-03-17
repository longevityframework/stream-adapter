
lazy val root = Project(
  id = "root",
  base = file("."),
  settings = BuildSettings.noPublishSettings).
  aggregate(core)

lazy val core = Project(
  id = "streamadapter",
  base = file("core"),
  settings = BuildSettings.buildSettings ++ Seq(

    fork in Test := false,

    libraryDependencies += Dependencies.typesafeConfigDep,

    libraryDependencies += Dependencies.akkaStreamDep      % Optional,
    libraryDependencies += Dependencies.catsDep            % Optional,
    libraryDependencies += Dependencies.catsIterateeDep    % Optional,
    libraryDependencies += Dependencies.fs2CoreDep         % Optional,
    libraryDependencies += Dependencies.playIterateeDep    % Optional,
    libraryDependencies += Dependencies.reactiveStreamsDep % Optional,

    libraryDependencies += Dependencies.catsDep            % Test,
    libraryDependencies += Dependencies.catsIterateeDep    % Test,
    libraryDependencies += Dependencies.fs2CoreDep         % Test,
    libraryDependencies += Dependencies.nScalaTimeDep      % Test,
    libraryDependencies += Dependencies.playIterateeDep    % Test,
    libraryDependencies += Dependencies.specs2Dep          % Test,
    libraryDependencies += Dependencies.reactiveStreamsDep % Test,

    homepage := BuildSettings.streamadapterHomepage,
    pomExtra := BuildSettings.streamadapterPomExtra

  ))
