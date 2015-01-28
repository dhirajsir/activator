/*
 Copyright (C) 2014 Typesafe, Inc <http://typesafe.com>
 */
define([
  'commons/websocket',
  'widgets/log/log',
  'text!./working.html'
], function(
  websocket,
  LogView,
  tpl
) {

  // var requestAnimationFrame = window.requestAnimationFrame || window.webkitRequestAnimationFrame || window.mozRequestAnimationFrame || window.msRequestAnimationFrame || window.oRequestAnimationFrame;

  return {
    render: function() {
      var dom = ko.bindhtml(tpl, {});
      var logs = $("#loading-logs", dom);
      var wrapper = $("article", dom)[0];

      var logView = LogView(function(m){
        var element = document.createElement("li");
        element.appendChild(document.createTextNode(m));
        return element;
      });

      logs.append(logView.render);

      websocket.subscribe({type: "sbt", subType: "CoreLogEvent"}).fork().each(function(message) {
        logView.push(message.event.entry.message);
        wrapper.scrollTop = 99999;
      });

      websocket.subscribe({ response: String }).fork().each(function(message) {
        switch(message.response) {
          case 'Status':
            logView.push(message.info);
            wrapper.scrollTop = 99999;
            break;
          case 'BadRequest':
            // TODO - Do better than an alert!
            window.alert('Unable to perform request: ' + message.errors.join(' \n'));
            $('#working, #open, #new').toggle();
            break;
          case 'RedirectToApplication':
            // NOTE - Comment this out if you want to debug showing logs!
            window.location.href = window.location.href.replace('home', 'app/'+message.appId+'/');
            break;
        }
      });

      return dom;
    }
  }

})
