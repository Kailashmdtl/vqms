/**
 * Inline form feedback shared by the account forms. Each helper returns a field status for <Field status={...}>:
 * { tone: 'valid' | 'invalid' | 'pending' | 'neutral', message } or null when nothing should be shown yet.
 * These only guide the user; the backend validates everything again on submit.
 */

/**
 * Confirm-password feedback. Nothing is shown until the user types a confirmation (or tries to submit), so a
 * fresh form never opens with an error or a premature tick. Never echoes the password itself.
 */
export function passwordMatchStatus(password, confirm, submitted = false) {
  if (!confirm) {
    return submitted && password ? { tone: 'invalid', message: 'Please confirm your password' } : null;
  }
  return confirm === password
    ? { tone: 'valid', message: 'Passwords match' }
    : { tone: 'invalid', message: 'Passwords do not match' };
}

/** A pragmatic shape check (something@domain.tld, no spaces); the backend's @Email check stays authoritative. */
const EMAIL_SHAPE = /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/;

export function isEmailShaped(email) {
  return EMAIL_SHAPE.test(email.trim());
}

/** Format feedback for an email field: nothing while empty, an error for an obviously malformed address. */
export function emailFormatStatus(email) {
  if (!email.trim()) return null;
  return isEmailShaped(email) ? null : { tone: 'invalid', message: 'Enter a valid email address, like name@company.com' };
}

/** The backend's duplicate-email rejection (409 "Email … is already registered"), recognised so it can be shown on the field. */
export function isDuplicateEmailError(error) {
  const data = error?.response?.data;
  return error?.response?.status === 409 && /^Email\b.*already registered/i.test(data?.message || '');
}

export const EMAIL_TAKEN = { tone: 'invalid', message: 'This email address is already registered' };
