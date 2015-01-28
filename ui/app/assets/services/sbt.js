/*
 Copyright (C) 2014 Typesafe, Inc <http://typesafe.com>
 */
define([
  './sbt/app',
  './sbt/dependencies',
  './sbt/tasks',
  './sbt/events',
  './sbt/configuration',
  './sbt/tests'
],function(
  app,
  dependencies,
  tasks,
  events,
  configuration,
  tests
) {

  return {
    app:            app,
    dependencies:   dependencies,
    tasks:          tasks,
    events:         events,
    configuration:  configuration,
    tests:          tests
  };

});
