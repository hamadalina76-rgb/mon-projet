import 'package:intl/intl.dart';

class PriceFormatter {
  static String formatPrice(double price) {
    return NumberFormat.currency(symbol: '\$', decimalDigits: 2).format(price);
  }
}
