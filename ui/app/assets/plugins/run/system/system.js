/*
 Copyright (C) 2014 Typesafe, Inc <http://typesafe.com>
 */
define([
  "main/plugins",
  "text!./system.html",
  "widgets/log/log",
  'commons/websocket',
  "css!./system",
  "css!widgets/modules/modules",
  "css!widgets/lists/logs"
], function(
  plugins,
  tpl,
  LogView,
  websocket
) {

  function scrollToBottom() {
    $(".logs")[0].scrollTop = 99999;
    State.memoLogsScroll('stick');
  }

  var hasLogs = ko.observable(false);

  var logView = LogView(function(m){
    if (!hasLogs()) hasLogs(true);
    var element = document.createElement("li");
    element.appendChild(document.createTextNode(m.event.entry.message));
    element.setAttribute('data-level', m.event.entry.level);
    element.setAttribute('data-type', m.event.entry.type);
    return element;
  }, 5000, 4000);


  // Websocket Handlers
  var logEvent = websocket.subscribe("type", "sbt");

  /**
  Logs, by execution/task
  */
  logEvent
    .matchOnAttribute("subType", "BackgroundJobLogEvent")
    .each(logView.push);

  var State = {
    logView: logView,
    ulLog: logView.render(),
    hasLogs: hasLogs,
    clear: function() {
      logView.clear();
    },
    memoLogsScroll: ko.observable(),
    scrollToBottom: scrollToBottom
  }

  return {
    render: function(){
      return ko.bindhtml(tpl, State)
    }
  }

});
