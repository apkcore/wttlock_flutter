class ControlAction {
  static const int UNLOCK = 3;
  static const int LOCK = 3 << 1;

  /// 卷闸门
  static const int ROLLING_GATE_UP = 1;
  static const int ROLLING_GATE_DOWN = 1 << 1;
  static const int ROLLING_GATE_PAUSE = 1 << 2;
  static const int ROLLING_GATE_LOCK = 1 << 3;

  ///
  static const int HOLD = 3 << 3;
}
