class BaseResponse<T> {
  final bool success;
  final String? message;
  final T? data;
  
  const BaseResponse({
    required this.success,
    this.message,
    this.data,
  });
}
