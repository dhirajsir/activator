/*
 Copyright (C) 2014 Typesafe, Inc <http://typesafe.com>
 */
define([
  "services/sbt",
  "main/plugins",
  "widgets/log/log",
  'commons/websocket',
  "text!./tasks.html",
  "css!./tasks",
  "css!widgets/modules/modules"
], function(
  sbt,
  plugins,
  LogView,
  websocket,
  tpl
) {

  function scrollToBottom() {
    $(".logs")[0].scrollTop = 99999;
    State.memoLogsScroll('stick');
  }

  var logView = LogView(function(m){
    var element = document.createElement("li");
    element.appendChild(document.createTextNode(m.event.entry.message));
    element.setAttribute('data-level', m.event.entry.level);
    element.setAttribute('data-type', m.event.entry.type);
    return element;
  }, 5000, 4000);


  /**
  Logs, by execution/task
  */
  // Websocket Handlers
  var logEvent = websocket.subscribe("type", "sbt");

  logEvent.matchOnAttribute("subType", "TaskLogEvent")
    .each(logView.push);

  logEvent.matchOnAttribute("subType", "CoreLogEvent")
    .each(logView.push);


  var State = {
    memoTaskScroll: ko.observable(),
    memoLogsScroll: ko.observable(),
    sbt: sbt,
    scrollToBottom: scrollToBottom,
    ulLog: logView.render(),
    clear: function() {
      logView.clear();
    },
    rerunTask: function(task, e) {
      e.preventDefault();
      e.stopPropagation();
      if (!task.finished()){
        sbt.tasks.kill(task);
      } else {
        sbt.tasks.requestExecution(task.command);
      }
    },
  };

  return {
    route: function(url, breadcrumb) {
    },

    render: function(){
      sbt.events.errorCounters.build(0);
      return ko.bindhtml(tpl, State)
    }
  }

});
