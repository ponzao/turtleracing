(function($) {
  $(function() {
    var name = prompt('Name: ');

    window.WebSocket = window.WebSocket || window.MozWebSocket;
    var ws = new WebSocket("ws://localhost:8008/");
    var drawingCanvas = document.getElementById('track');
    var context = drawingCanvas.getContext('2d');

    var draw = function(turtles) {
      context.clearRect(0, 0, 320, 240);
      _.each(turtles, function(turtle) {
        context.beginPath();
        context.fillStyle = '#006400';
        context.arc(turtle.position[0], turtle.position[1], 2, 0, Math.PI * 2, true);
        context.closePath();
        context.fill();
      });
    }

    ws.onopen = function() {
      console.log('opened');
      ws.send(name);
    };
    ws.onmessage = function(ev) {
      console.log(ev.data);
      draw($.parseJSON(ev.data));
    };
    ws.onclose = function() {
      console.log('closed');
    };
    $(window).keydown(function(ev) {
      var key = ev.which;
      if (key === 38) {
        console.log('moving forward');
        ws.send('forward');
        //ws.send('backward');
      } 
      
      if (key === 37) {
        console.log('turning left');
        ws.send('left');
      } else if (key === 39) {
        console.log('turning right');
        ws.send('right');
      }
    });
  });
})(jQuery);

