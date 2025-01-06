export interface Query<T, E> {
  isLoading: boolean;
  data?: T;
  error?: E;
}
