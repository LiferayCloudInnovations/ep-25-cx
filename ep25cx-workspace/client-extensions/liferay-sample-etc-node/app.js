#!/usr/bin/env node

var amqp = require('amqplib/callback_api');

var express = require('express')

var connectOptions = {
    "hostname": "liferay-default-rabbitmq",
    "username": "lfrrabbitmq",
    "password": "lfrrabbitmq",
    "vhost": "main.dxp.localtest.me"
}

const app = express();

app.use(express.json());


app.get("/ready", (req, res) => {
	res.send('READY');
});


const serverPort = 3001;
app.listen(serverPort, () => {
	console.log(`App listening on ${serverPort}`);
});


amqp.connect(connectOptions, function(error0, connection) {
    if (error0) {
        throw error0;
    }
    connection.createChannel(function(error1, channel) {
        if (error1) {
            throw error1;
        }

        var queue = 'login.events.postEvent.default';

        channel.assertQueue(queue, {
            durable: true
        });

        console.log(" [*] Waiting for messages in %s. To exit press CTRL+C", queue);

        channel.consume(queue, function(msg) {
            console.log(" [x] Received %s", msg.content.toString());
        }, {
            noAck: true
        });
    });
});

