// Replace the existing customer fetch method with a stream
Stream<List<Customer>> streamCustomers() {
  return _firestore
      .collection('customers')
      .snapshots()
      .map((snapshot) => snapshot.docs
          .map((doc) => Customer.fromMap(doc.data(), doc.id))
          .toList());
}

// Add this method for stock items
Stream<List<StockItem>> streamStockItems() {
  return _firestore
      .collection('stock')
      .snapshots()
      .map((snapshot) => snapshot.docs
          .map((doc) => StockItem.fromMap(doc.data(), doc.id))
          .toList());
} 