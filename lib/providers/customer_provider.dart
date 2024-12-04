class CustomerProvider extends ChangeNotifier {
  final FirestoreService _firestoreService = FirestoreService();
  List<Customer> _customers = [];
  StreamSubscription? _customersSubscription;

  List<Customer> get customers => _customers;

  CustomerProvider() {
    // Initialize the real-time listener
    _initializeCustomersListener();
  }

  void _initializeCustomersListener() {
    _customersSubscription?.cancel();
    _customersSubscription = _firestoreService.streamCustomers().listen(
      (customersList) {
        _customers = customersList;
        notifyListeners();
      },
      onError: (error) {
        print('Error streaming customers: $error');
      },
    );
  }

  @override
  void dispose() {
    _customersSubscription?.cancel();
    super.dispose();
  }
} 