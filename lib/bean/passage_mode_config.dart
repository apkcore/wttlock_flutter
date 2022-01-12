class PassageModeConfig {
  PassageModeType? modeType;
  String? repeatWeekOrDays;
  int? month;
  int? startDate;
  int? endDate;

  PassageModeType? getModeType() {
    return modeType;
  }

  void setModeType(PassageModeType modeType) {
    this.modeType = modeType;
  }

  String ?getRepeatWeekOrDays() {
    return repeatWeekOrDays;
  }

  void setRepeatWeekOrDays(String repeatWeekOrDays) {
    this.repeatWeekOrDays = repeatWeekOrDays;
  }

  int? getMonth() {
    return month;
  }

  void setMonth(int month) {
    this.month = month;
  }

  int? getStartDate() {
    return startDate;
  }

  void setStartDate(int startDate) {
    this.startDate = startDate;
  }

  int? getEndDate() {
    return endDate;
  }

  void setEndDate(int endDate) {
    this.endDate = endDate;
  }
}

class PassageModeType {
  static final int Weekly = 1;
  static final int Monthly = 2;

  int? value;

  PassageModeType(int value) {
    this.value = value;
  }

  int? getValue() {
    return value;
  }
}
