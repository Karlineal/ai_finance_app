part of 'transaction_bloc.dart';

abstract class TransactionEvent extends Equatable {
  const TransactionEvent();

  @override
  List<Object?> get props => [];
}

class LoadTransactions extends TransactionEvent {
  final DateTime? startDate;
  final DateTime? endDate;

  const LoadTransactions({this.startDate, this.endDate});

  @override
  List<Object?> get props => [startDate, endDate];
}

class AddTransaction extends TransactionEvent {
  final Transaction transaction;

  const AddTransaction(this.transaction);

  @override
  List<Object?> get props => [transaction];
}

class UpdateTransaction extends TransactionEvent {
  final Transaction transaction;

  const UpdateTransaction(this.transaction);

  @override
  List<Object?> get props => [transaction];
}

class DeleteTransaction extends TransactionEvent {
  final String id;

  const DeleteTransaction(this.id);

  @override
  List<Object?> get props => [id];
}

class LoadMonthlyStats extends TransactionEvent {
  final int year;
  final int month;

  const LoadMonthlyStats(this.year, this.month);

  @override
  List<Object?> get props => [year, month];
}

class LoadCategories extends TransactionEvent {
  final TransactionType? type;

  const LoadCategories({this.type});

  @override
  List<Object?> get props => [type];
}