export interface Mutation<T, E> {
  isSuccess: boolean;
  data?: T;
  error?: E;
}
