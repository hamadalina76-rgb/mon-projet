class Validators {
  static bool isValidEmail(String email) {
    final emailRegex = RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$');
    return emailRegex.hasMatch(email);
  }
  
  static bool isValidPhone(String phone) {
    final phoneRegex = RegExp(r'^\+?[0-9]{8,15}$');
    return phoneRegex.hasMatch(phone);
  }
  
  static bool isValidPassword(String password) {
    return password.length >= 6;
  }
  
  static bool isEmpty(String? value) {
    return value == null || value.trim().isEmpty;
  }
}
