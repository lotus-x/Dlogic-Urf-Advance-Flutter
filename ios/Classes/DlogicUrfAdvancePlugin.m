#import "DlogicUrfAdvancePlugin.h"
#if __has_include(<dlogic_urf_advance/dlogic_urf_advance-Swift.h>)
#import <dlogic_urf_advance/dlogic_urf_advance-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "dlogic_urf_advance-Swift.h"
#endif

@implementation DlogicUrfAdvancePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftDlogicUrfAdvancePlugin registerWithRegistrar:registrar];
}
@end
