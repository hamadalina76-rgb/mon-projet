import 'package:intl/intl.dart';

class CurrencyFormatter {
  static String format(double amount, {String currency = 'TND'}) {
    final formatter = NumberFormat.currency(
      locale: 'fr_FR',
      symbol: currency,
      decimalDigits: 3,
    );
    return formatter.format(amount);
  }
}
