import { useCallback, useEffect, useRef, useState } from 'react';
import { EMAIL_TAKEN, emailFormatStatus } from '../utils/validation.js';

const CHECKING = { tone: 'pending', message: 'Checking availability…' };
const AVAILABLE = { tone: 'valid', message: 'Email address is available' };
// The server still rejects a duplicate on save, so a failed check only loses the early warning
const FAILED = { tone: 'neutral', message: 'Could not check availability right now.' };

/**
 * Live "is this email already registered?" feedback for an email field.
 *
 * `check(email)` resolves true when the (trimmed, lower-cased) address is already taken. It is called only for a
 * well-formed address, after `delay` ms without typing; answers to an address the user has since changed are
 * ignored, so quick typing can never show a stale result. A green tick appears only after `check` confirms the
 * address is free; a failed check never claims it is free. `skip` turns the check off (e.g. an unchanged address).
 *
 * Returns { status, taken, retry }: `status` for <Field status>, `taken` true once confirmed taken, `retry()`
 * after a failed check.
 */
export function useEmailAvailability(email, { check, skip = false, delay = 400 }) {
  const normalized = email.trim().toLowerCase();
  const formatStatus = emailFormatStatus(email);
  const [result, setResult] = useState({ key: null, state: null });
  const [attempt, setAttempt] = useState(0);
  const latest = useRef(0);
  const checkRef = useRef(check);
  checkRef.current = check;

  const shouldCheck = Boolean(normalized) && !formatStatus && !skip;
  const key = `${normalized}#${attempt}`; // the exact question the shown answer must belong to
  useEffect(() => {
    const id = ++latest.current; // any newer run makes this one's answer stale
    if (!shouldCheck) return undefined;
    const timer = setTimeout(() => {
      Promise.resolve()
        .then(() => checkRef.current(normalized))
        .then((taken) => { if (id === latest.current) setResult({ key, state: taken ? 'taken' : 'available' }); })
        .catch(() => { if (id === latest.current) setResult({ key, state: 'failed' }); });
    }, delay);
    return () => clearTimeout(timer);
  }, [key, normalized, shouldCheck, delay]);

  const retry = useCallback(() => setAttempt((n) => n + 1), []);

  if (!normalized) return { status: null, taken: false, retry };
  if (formatStatus) return { status: formatStatus, taken: false, retry };
  if (skip) return { status: null, taken: false, retry };
  if (result.key !== key) return { status: CHECKING, taken: false, retry };
  if (result.state === 'taken') return { status: EMAIL_TAKEN, taken: true, retry };
  if (result.state === 'available') return { status: AVAILABLE, taken: false, retry };
  return { status: FAILED, taken: false, retry, failed: true };
}
