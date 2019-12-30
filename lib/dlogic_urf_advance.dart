import 'dart:async';

import 'package:flutter/services.dart';

enum UrfBeepMode {
  None,
  Mode1,
  Mode2,
  Mode3,
  Mode4,
  Mode5,
}

enum UrfLightMode {
  None,
  Mode1,
  Mode2,
  Mode3,
  Mode4,
}

class DlogicUrfAdvance {
  static const MethodChannel _channel =
      const MethodChannel('lotus/dlogic_urf_advance');
  static const EventChannel _stream =
      const EventChannel('lotus/dlogic_urf_advance_stream');

  Stream<String> _onNfcDiscovered;

  Future<bool> init() => _channel.invokeMethod('init');
  Future<bool> connect() => _channel.invokeMethod('connect');
  Future<bool> disconnect() => _channel.invokeMethod('disconnect');
  Future<String> getReaderType() => _channel.invokeMethod('getReaderType');
  Future<bool> emitUiSignal() => _channel.invokeMethod('emitUiSignal');
  Future<bool> enterSleepMode() => _channel.invokeMethod('enterSleepMode');
  Future<bool> leaveSleepMode() => _channel.invokeMethod('leaveSleepMode');
  Future<bool> changeBeepMode(UrfBeepMode mode) =>
      _channel.invokeMethod('changeBeepMode', {'mode': mode.index});
  Future<bool> changeLightMode(UrfLightMode mode) =>
      _channel.invokeMethod('changeLightMode', {'mode': mode.index});

  Stream<String> onNfcDiscover() {
    if (_onNfcDiscovered == null) {
      _onNfcDiscovered = _stream.receiveBroadcastStream().cast<String>();
    }
    return _onNfcDiscovered;
  }
}
