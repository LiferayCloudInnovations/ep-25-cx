var client;
import('https://ga.jspm.io/npm:@stomp/stompjs@7.0.0/esm6/index.js').then((mod) => {
  console.log(mod);

  client = new mod.Client({
    brokerURL: `ws://${location.hostname}:15674/ws`,
    connectHeaders: {
      login: 'lfrrabbitmq',
      passcode: 'lfrrabbitmq',
      host: location.hostname,
    },
    onConnect: () => {
      client.subscribe('C_EP25SampleEvent', message =>
        console.log(`Received: ${message.body}`)
      );
      client.publish({ destination: 'C_EP25SampleEvent', body: 'Response Message' });
    },
  });

  client.activate();
});

client.deactivate();
