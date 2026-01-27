export type ApiError = {
  code: string;
  message: string;
  details?: unknown;
};

export function ok<T>(data: T) {
  return {
    success: true,
    data
  };
}

export function fail(error: ApiError) {
  return {
    success: false,
    error
  };
}
