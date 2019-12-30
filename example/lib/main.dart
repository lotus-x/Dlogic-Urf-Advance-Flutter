import 'dart:async';

import 'package:flutter/material.dart';

import 'package:flutter/services.dart';
import 'package:dlogic_urf_advance/dlogic_urf_advance.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  DlogicUrfAdvance _dlogicUrfAdvance = DlogicUrfAdvance();
  StreamSubscription _subscription;

  String _text = 'Unknown';

  int _beepRadioValue = 0;
  int _lightRadioValue = 0;

  @override
  void initState() {
    super.initState();

    _init();
  }

  _init() async {
    try {
      _dlogicUrfAdvance.init();
      try {
        var result = await _dlogicUrfAdvance.connect();
        debugPrint('CONNECT: ' + result.toString());
      } catch (e) {
        debugPrint('ERROR: ' + e.message);
      }
      _subscription = _dlogicUrfAdvance.onNfcDiscover().listen((data) {
        debugPrint("CARD: " + data.toString());
        setState(() {
          _text = data;
        });
      }, onError: (error) {
        debugPrint("READ ERROR: " + error.toString());
      });
    } on PlatformException {
      setState(() {
        _text = 'Failed to get platform version.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Padding(
                padding: EdgeInsets.symmetric(vertical: 20),
                child: Text(_text),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Column(
                    children: <Widget>[
                      Radio(
                        value: 0,
                        groupValue: _beepRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeBeepMode(UrfBeepMode.None);
                          setState(() {
                            _beepRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 1,
                        groupValue: _beepRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeBeepMode(UrfBeepMode.Mode1);
                          setState(() {
                            _beepRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 2,
                        groupValue: _beepRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeBeepMode(UrfBeepMode.Mode2);
                          setState(() {
                            _beepRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 3,
                        groupValue: _beepRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeBeepMode(UrfBeepMode.Mode3);
                          setState(() {
                            _beepRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 4,
                        groupValue: _beepRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeBeepMode(UrfBeepMode.Mode4);
                          setState(() {
                            _beepRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 5,
                        groupValue: _beepRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeBeepMode(UrfBeepMode.Mode5);
                          setState(() {
                            _beepRadioValue = value;
                          });
                        },
                      ),
                    ],
                  ),
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: <Widget>[
                      Padding(
                        padding: EdgeInsets.only(top: 15),
                        child: Text('None'),
                      ),
                      Padding(
                        padding: EdgeInsets.only(top: 35),
                        child: Text('Mode1'),
                      ),
                      Padding(
                        padding: EdgeInsets.only(top: 30),
                        child: Text('Mode2'),
                      ),
                      Padding(
                        padding: EdgeInsets.only(top: 33),
                        child: Text('Mode3'),
                      ),
                      Padding(
                        padding: EdgeInsets.only(top: 30),
                        child: Text('Mode4'),
                      ),
                      Padding(
                        padding: EdgeInsets.only(top: 30),
                        child: Text('Mode5'),
                      ),
                    ],
                  ),
                  Column(
                    children: <Widget>[
                      Radio(
                        value: 0,
                        groupValue: _lightRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeLightMode(UrfLightMode.None);
                          setState(() {
                            _lightRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 1,
                        groupValue: _lightRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeLightMode(UrfLightMode.Mode1);
                          setState(() {
                            _lightRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 2,
                        groupValue: _lightRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeLightMode(UrfLightMode.Mode2);
                          setState(() {
                            _lightRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 3,
                        groupValue: _lightRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeLightMode(UrfLightMode.Mode3);
                          setState(() {
                            _lightRadioValue = value;
                          });
                        },
                      ),
                      Radio(
                        value: 4,
                        groupValue: _lightRadioValue,
                        onChanged: (value) {
                          _dlogicUrfAdvance.changeLightMode(UrfLightMode.Mode4);
                          setState(() {
                            _lightRadioValue = value;
                          });
                        },
                      ),
                    ],
                  ),
                ],
              ),
              Padding(
                padding: EdgeInsets.only(top: 20),
                child: FlatButton(
                  onPressed: () {
                    _dlogicUrfAdvance.emitUiSignal();
                  },
                  color: Colors.blue,
                  child: Text('Emit UI Signal'),
                ),
              ),
              Padding(
                padding: EdgeInsets.only(top: 20),
                child: FlatButton(
                  onPressed: () {
                    _subscription.cancel();
                    _dlogicUrfAdvance.enterSleepMode();
                  },
                  color: Colors.blue,
                  child: Text('Enter Sleep Mode'),
                ),
              ),
              Padding(
                padding: EdgeInsets.only(top: 20),
                child: FlatButton(
                  onPressed: () async {
                    await _dlogicUrfAdvance.leaveSleepMode();
                    _subscription =
                        _dlogicUrfAdvance.onNfcDiscover().listen((data) {
                      debugPrint("CARD: " + data.toString());
                      setState(() {
                        _text = data;
                      });
                    }, onError: (error) {
                      debugPrint("READ ERROR: " + error.toString());
                    });
                  },
                  color: Colors.blue,
                  child: Text('Leave Sleep Mode'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
