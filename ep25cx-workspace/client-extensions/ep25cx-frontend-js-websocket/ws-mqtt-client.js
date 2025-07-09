var script = document.createElement('script');
script.src = 'https://cdnjs.cloudflare.com/ajax/libs/paho-mqtt/1.1.0/paho-mqtt.min.js';
document.head.appendChild(script);

var client;
script.onload = function() {
  if (typeof Paho !== 'undefined' && typeof Paho.Client === 'function') {
    client = new Paho.Client(
      location.hostname,
      Number(15675),
      '/ws',
      "myclientid_" + parseInt(Math.random() * 100, 10)
    );

    client.onConnectionLost = function (responseObject) {
      console.log("CONNECTION LOST - " + responseObject.errorMessage);
    };

    client.onMessageArrived = function (message) {
      console.log("RECEIVE ON " + message.destinationName + " PAYLOAD " + message.payloadString);
    };

    var options = {
      keepAliveInterval: 30,
      password: 'lfrrabbitmq',
      timeout: 3,
      userName: `${location.hostname}:lfrrabbitmq`,
      onSuccess: function () {
          console.log("CONNECTION SUCCESS");
          client.subscribe("C.EP25SampleEvent", {qos: 1});
      },
      onFailure: function (message) {
          console.log("CONNECTION FAILURE - " + message.errorMessage);
      }
    };

    client.connect(options);
  }
};

client.deactivate();