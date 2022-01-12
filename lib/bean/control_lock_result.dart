class ControlLockResult{
  int? lockAction;
  int? battery;
  int? uniqueId;

  @override
  String toString() {
    return 'ControlLockResult{lockAction: $lockAction, battery: $battery, uniqueId: $uniqueId}';
  }
}