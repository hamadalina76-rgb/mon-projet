import 'package:drift/drift.dart';

class Users extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get userId => text().unique()();
  TextColumn get name => text()();
  TextColumn get email => text()();
  TextColumn get phone => text()();
  TextColumn get profileImage => text().nullable()();
  DateTimeColumn get createdAt => dateTime()();
  DateTimeColumn get updatedAt => dateTime()();
}

class Orders extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get orderId => text().unique()();
  TextColumn get userId => text()();
  TextColumn get restaurantId => text()();
  TextColumn get status => text()();
  RealColumn get totalAmount => real()();
  TextColumn get items => text()(); // JSON string
  TextColumn get deliveryAddress => text()();
  DateTimeColumn get createdAt => dateTime()();
  DateTimeColumn get updatedAt => dateTime()();
}

class Addresses extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get addressId => text().unique()();
  TextColumn get userId => text()();
  TextColumn get label => text()();
  TextColumn get fullAddress => text()();
  RealColumn get latitude => real()();
  RealColumn get longitude => real()();
  BoolColumn get isDefault => boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt => dateTime()();
}

class Restaurants extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get restaurantId => text().unique()();
  TextColumn get name => text()();
  TextColumn get description => text()();
  TextColumn get image => text().nullable()();
  RealColumn get rating => real()();
  TextColumn get categories => text()(); // JSON array
  RealColumn get latitude => real()();
  RealColumn get longitude => real()();
  BoolColumn get isOpen => boolean()();
  DateTimeColumn get cachedAt => dateTime()();
}

class CartItems extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get productId => text()();
  TextColumn get restaurantId => text()();
  TextColumn get name => text()();
  RealColumn get price => real()();
  IntColumn get quantity => integer()();
  TextColumn get options => text().nullable()(); // JSON string
  DateTimeColumn get addedAt => dateTime()();
}
