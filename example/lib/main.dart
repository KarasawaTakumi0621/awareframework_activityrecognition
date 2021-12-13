import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:awareframework_activityrecognition/awareframework_activityrecognition.dart';
import 'package:awareframework_core/awareframework_core.dart';

void main() => runApp(new MyApp());

class MyApp extends StatefulWidget {
  late ActivityRecognitionSensor sensor;
  ActivityRecognitionData data = ActivityRecognitionData();
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool sensorState = true;

  @override
  void initState() {
    super.initState();

    var config = ActivityRecognitionSensorConfig();
    // config.usageAppDisplaynames = ["com.twitter.android", "com.facebook.orca", "com.facebook.katana", "com.instagram.android", "jp.naver.line.android", "com.ss.android.ugc.trill"];
    // config.usageAppEventTypes = [1,2];

    // // init sensor without a context-card
    widget.sensor = new ActivityRecognitionSensor.init(config);

    // card = new AccelerometerCard(sensor: sensor,);
  }

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin Example App'),
        ),
        body: Column(
          children: [
            Text("最新のデータ"),

            Text("Timestamp: ${widget.data.timestamp}"),
            Text("TimeZone: ${widget.data.timezone}"),
            Text("ActivityTransition: ${widget.data.activityTransiton}"),
            Text("DetectedActivity: ${widget.data.detectedActivity}"),

            TextButton(
                onPressed: () {
                  widget.sensor.start();
                  widget.sensor.onDataChanged.listen((data) {
                    setState(() {
                      widget.data = data;
                      print(data);
                    });
                  });
                },
                child: Text("Start")),
            TextButton(
                onPressed: () {
                  widget.sensor.stop();
                },
                child: Text("Stop")),
            TextButton(
                onPressed: () {
                  widget.sensor.sync();
                },
                child: Text("Sync")),
          ],
        ),
      ),
    );
  }
}
