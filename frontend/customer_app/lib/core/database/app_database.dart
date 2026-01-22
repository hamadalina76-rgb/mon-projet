import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'tables.dart';

part 'app_database.g.dart';

@DriftDatabase(tables: [Users, Orders, Addresses, Restaurants, CartItems])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 1;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (Migrator m) async {
          await m.createAll();
        },
        onUpgrade: (Migrator m, int from, int to) async {
          // Handle migrations here
        },
      );

  // User operations
  Future<int> insertUser(UsersCompanion user) => into(users).insert(user);
  Future<User?> getUserById(String userId) =>
      (select(users)..where((tbl) => tbl.userId.equals(userId))).getSingleOrNull();
  Future<bool> updateUser(UsersCompanion user) => update(users).replace(user);
  Future<int> deleteUser(String userId) =>
      (delete(users)..where((tbl) => tbl.userId.equals(userId))).go();

  // Order operations
  Future<int> insertOrder(OrdersCompanion order) => into(orders).insert(order);
  Future<List<Order>> getUserOrders(String userId) =>
      (select(orders)..where((tbl) => tbl.userId.equals(userId))
        ..orderBy([(t) => OrderingTerm(expression: t.createdAt, mode: OrderingMode.desc)])).get();
  Future<bool> updateOrder(OrdersCompanion order) => update(orders).replace(order);

  // Address operations
  Future<int> insertAddress(AddressesCompanion address) => into(addresses).insert(address);
  Future<List<AddressesData>> getUserAddresses(String userId) =>
      (select(addresses)..where((tbl) => tbl.userId.equals(userId))).get();
  Future<AddressesData?> getDefaultAddress(String userId) =>
      (select(addresses)..where((tbl) => tbl.userId.equals(userId) & tbl.isDefault.equals(true))).getSingleOrNull();
  Future<bool> updateAddress(AddressesCompanion address) => update(addresses).replace(address);
  Future<int> deleteAddress(String addressId) =>
      (delete(addresses)..where((tbl) => tbl.addressId.equals(addressId))).go();

  // Restaurant operations
  Future<int> insertRestaurant(RestaurantsCompanion restaurant) => into(restaurants).insert(restaurant);
  Future<List<Restaurant>> getAllRestaurants() => select(restaurants).get();
  Future<Restaurant?> getRestaurantById(String restaurantId) =>
      (select(restaurants)..where((tbl) => tbl.restaurantId.equals(restaurantId))).getSingleOrNull();

  // Cart operations
  Future<int> insertCartItem(CartItemsCompanion item) => into(cartItems).insert(item);
  Future<List<CartItem>> getCartItems() => select(cartItems).get();
  Future<bool> updateCartItem(CartItemsCompanion item) => update(cartItems).replace(item);
  Future<int> deleteCartItem(int id) =>
      (delete(cartItems)..where((tbl) => tbl.id.equals(id))).go();
  Future<int> clearCart() => delete(cartItems).go();
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'customer_app.db'));
    return NativeDatabase(file);
  });
}
