import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../database/database_helper.dart';
import '../models/transaction.dart';

part 'transaction_event.dart';
part 'transaction_state.dart';

class TransactionBloc extends Bloc<TransactionEvent, TransactionState> {
  final DatabaseHelper _db = DatabaseHelper.instance;

  TransactionBloc() : super(TransactionInitial()) {
    on<LoadTransactions>(_onLoadTransactions);
    on<AddTransaction>(_onAddTransaction);
    on<UpdateTransaction>(_onUpdateTransaction);
    on<DeleteTransaction>(_onDeleteTransaction);
    on<LoadMonthlyStats>(_onLoadMonthlyStats);
    on<LoadCategories>(_onLoadCategories);
  }

  Future<void> _onLoadTransactions(
    LoadTransactions event,
    Emitter<TransactionState> emit,
  ) async {
    emit(TransactionLoading());
    try {
      final now = DateTime.now();
      final List<Transaction> transactions = await _db.getTransactions(
        startDate: DateTime(now.year, now.month, 1),
        endDate: DateTime(now.year, now.month + 1, 0),
      );
      final stats = await _db.getMonthlyStats(now.year, now.month);
      final categories = await _db.getCategories();

      emit(TransactionLoaded(
        transactions: transactions,
        monthlyExpense: stats['expense'] ?? 0,
        monthlyIncome: stats['income'] ?? 0,
        categories: categories,
      ));
    } catch (e) {
      emit(TransactionError(e.toString()));
    }
  }

  Future<void> _onAddTransaction(
    AddTransaction event,
    Emitter<TransactionState> emit,
  ) async {
    try {
      await _db.insertTransaction(event.transaction);
      add(const LoadTransactions());
    } catch (e) {
      emit(TransactionError(e.toString()));
    }
  }

  Future<void> _onUpdateTransaction(
    UpdateTransaction event,
    Emitter<TransactionState> emit,
  ) async {
    try {
      await _db.updateTransaction(event.transaction);
      add(const LoadTransactions());
    } catch (e) {
      emit(TransactionError(e.toString()));
    }
  }

  Future<void> _onDeleteTransaction(
    DeleteTransaction event,
    Emitter<TransactionState> emit,
  ) async {
    try {
      await _db.deleteTransaction(event.id);
      add(const LoadTransactions());
    } catch (e) {
      emit(TransactionError(e.toString()));
    }
  }

  Future<void> _onLoadMonthlyStats(
    LoadMonthlyStats event,
    Emitter<TransactionState> emit,
  ) async {
    try {
      final stats = await _db.getMonthlyStats(event.year, event.month);
      final categoryStats = await _db.getCategoryStats(
        event.year,
        event.month,
        TransactionType.expense,
      );

      if (state is TransactionLoaded) {
        final currentState = state as TransactionLoaded;
        emit(currentState.copyWith(
          monthlyExpense: stats['expense'] ?? 0,
          monthlyIncome: stats['income'] ?? 0,
          categoryStats: categoryStats,
        ));
      }
    } catch (e) {
      emit(TransactionError(e.toString()));
    }
  }

  Future<void> _onLoadCategories(
    LoadCategories event,
    Emitter<TransactionState> emit,
  ) async {
    try {
      final categories = await _db.getCategories(type: event.type);
      if (state is TransactionLoaded) {
        final currentState = state as TransactionLoaded;
        emit(currentState.copyWith(categories: categories));
      }
    } catch (e) {
      emit(TransactionError(e.toString()));
    }
  }
}