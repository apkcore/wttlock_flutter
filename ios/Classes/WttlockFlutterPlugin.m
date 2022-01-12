#import "WttlockFlutterPlugin.h"
#if __has_include(<wttlock_flutter/wttlock_flutter-Swift.h>)
#import <wttlock_flutter/wttlock_flutter-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "wttlock_flutter-Swift.h"
#endif

@implementation WttlockFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftWttlockFlutterPlugin registerWithRegistrar:registrar];
}
@end
