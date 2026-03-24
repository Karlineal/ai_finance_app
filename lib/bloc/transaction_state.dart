part of 'transaction_bloc.dart';

abstract class TransactionState extends Equatable {
  const TransactionState();

  @override
  List<Object?> get props => [];
}

class TransactionInitial extends TransactionState {}

class TransactionLoading extends TransactionState {}

class TransactionLoaded extends TransactionState {
  final List<Transaction> transactions;
  final double monthlyExpense;
  final double monthlyIncome;
  final List<Category> categories;
  final List<Map<String, dynamic>>? categoryStats;

  const TransactionLoaded({
    required this.transactions,
    required this.monthlyExpense,
    required this.monthlyIncome,
    required this.categories,
    this.categoryStats,
  });

  double get balance => monthlyIncome - monthlyExpense;

  TransactionLoaded copyWith({
    List<Transaction>? transactions,
    double? monthlyExpense,
    double? monthlyIncome,
    List<Category>? categories,
    List<Map<String, dynamic>>? categoryStats,
  }) {
    return TransactionLoaded(
      transactions: transactions ?? this.transactions,
      monthlyExpense: monthlyExpense ?? this.monthlyExpense,
      monthlyIncome: monthlyIncome ?? this.monthlyIncome,
      categories: categories ?? this.categories,
      categoryStats: categoryStats ?? this.categoryStats,
    );
  }

  @override
  List<Object?> get props => [
        transactions,
        monthlyExpense,
        monthlyIncome,
        categories,
        categoryStats,
      ];
}

class TransactionError extends TransactionState {
  final String message;

  const TransactionError(this.message);

  @override
  List<Object?> get props => [message];
}