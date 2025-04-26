export class CredentialData {
  readonly credentialSubject?: Record<string, unknown>;
  readonly evidence?: Record<string, unknown>[];
  [K: string]: unknown;
}
