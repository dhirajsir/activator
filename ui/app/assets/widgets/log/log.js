/*
 Copyright (C) 2015 Typesafe, Inc <http://typesafe.com>
 */
define(function(){

  return function(childRender, maxSize, minSize) {

    maxSize = maxSize || 2000;
    minSize = maxSize * 8 / 10;

    var parent = document.createElement("ul");

    var buffer, lastTime = 0;
    function getDomBuffer(){
      if (!buffer){
        buffer = document.createDocumentFragment();
      }
      return buffer;
    }

    function push(message){
      var now = new Date();
      getDomBuffer().appendChild(childRender(message));
      if (now - lastTime > 60) {
        append(now);
      }
    }

    function append(now){
      lastTime = now;
      parent.appendChild(getDomBuffer());
      if(parent.children.length > maxSize){
        [].slice.call(parent.children, 0, minSize).forEach(function(el) {
          parent.removeChild(el);
        });
      }
    }

    function render(){
      return parent;
    }

    function clear(){
      parent.innerHTML = "";
    }

    return {
      push: push,
      render: render,
      clear: clear
    }
  }

});
