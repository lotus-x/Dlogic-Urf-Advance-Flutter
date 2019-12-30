import Flutter
import UIKit

public class SwiftDlogicUrfAdvancePlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "dlogic_urf_advance", binaryMessenger: registrar.messenger())
    let instance = SwiftDlogicUrfAdvancePlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
