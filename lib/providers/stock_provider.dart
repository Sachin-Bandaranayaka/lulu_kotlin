class StockProvider extends ChangeNotifier {
  final FirestoreService _firestoreService = FirestoreService();
  List<StockItem> _stockItems = [];
  StreamSubscription? _stockSubscription;

  List<StockItem> get stockItems => _stockItems;

  StockProvider() {
    _initializeStockListener();
  }

  void _initializeStockListener() {
    _stockSubscription?.cancel();
    _stockSubscription = _firestoreService.streamStockItems().listen(
      (stockList) {
        _stockItems = stockList;
        notifyListeners();
      },
      onError: (error) {
        print('Error streaming stock items: $error');
      },
    );
  }

  @override
  void dispose() {
    _stockSubscription?.cancel();
    super.dispose();
  }
} 