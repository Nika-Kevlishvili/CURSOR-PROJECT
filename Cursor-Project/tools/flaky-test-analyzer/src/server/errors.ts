export class HttpError extends Error {
  constructor(
    public status: number,
    message: string,
    public code: string,
  ) {
    super(message);
    this.name = 'HttpError';
  }
}

export class PolicyError extends HttpError {
  constructor(message: string, code = 'PATCH_POLICY') {
    super(409, message, code);
    this.name = 'PolicyError';
  }
}
