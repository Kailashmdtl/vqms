import { act, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useEmailAvailability } from '../hooks/useEmailAvailability.js';
import { emailFormatStatus, isDuplicateEmailError, passwordMatchStatus } from './validation.js';

describe('passwordMatchStatus', () => {
  it('stays quiet until a confirmation is typed, then reports match or mismatch', () => {
    expect(passwordMatchStatus('Secret12', '')).toBeNull();
    expect(passwordMatchStatus('Secret12', 'Sec')).toEqual({ tone: 'invalid', message: 'Passwords do not match' });
    expect(passwordMatchStatus('Secret12', 'Secret12')).toEqual({ tone: 'valid', message: 'Passwords match' });
  });

  it('asks for the confirmation only after a submit attempt', () => {
    expect(passwordMatchStatus('Secret12', '', true)).toEqual({ tone: 'invalid', message: 'Please confirm your password' });
    expect(passwordMatchStatus('', '', true)).toBeNull();
  });

  it('never repeats the password in its message', () => {
    expect(JSON.stringify(passwordMatchStatus('Secret12', 'Secret13'))).not.toContain('Secret');
  });
});

describe('email helpers', () => {
  it('flags obviously malformed addresses only', () => {
    expect(emailFormatStatus('')).toBeNull();
    expect(emailFormatStatus('name@company.com')).toBeNull();
    for (const bad of ['name', 'name@', '@company.com', 'name@company', 'na me@company.com']) {
      expect(emailFormatStatus(bad)?.tone).toBe('invalid');
    }
  });

  it("recognises the backend's duplicate-email conflict and nothing else", () => {
    const conflict = (message) => ({ response: { status: 409, data: { message } } });
    expect(isDuplicateEmailError(conflict('Email a@b.co is already registered'))).toBe(true);
    expect(isDuplicateEmailError(conflict('Username acme is already taken'))).toBe(false);
    expect(isDuplicateEmailError({ response: { status: 500, data: { message: 'Email a@b.co is already registered' } } })).toBe(false);
  });
});

describe('useEmailAvailability', () => {
  afterEach(() => vi.useRealTimers());
  const flush = async (ms) => { await act(async () => { vi.advanceTimersByTime(ms); await Promise.resolve(); await Promise.resolve(); }); };

  it('does not call the check for an empty or malformed address', async () => {
    vi.useFakeTimers();
    const check = vi.fn();
    const { result, rerender } = renderHook(({ email }) => useEmailAvailability(email, { check }), { initialProps: { email: '' } });
    expect(result.current.status).toBeNull();
    rerender({ email: 'bad@' });
    await flush(1000);
    expect(check).not.toHaveBeenCalled();
    expect(result.current.status.tone).toBe('invalid');
  });

  it('shows checking, then available or taken, and ignores an answer for an older address', async () => {
    vi.useFakeTimers();
    let resolveOld;
    const check = vi.fn((email) => (email === 'old@x.com' ? new Promise((r) => { resolveOld = r; }) : Promise.resolve(false)));
    const { result, rerender } = renderHook(({ email }) => useEmailAvailability(email, { check }), { initialProps: { email: 'old@x.com' } });
    expect(result.current.status.tone).toBe('pending');
    await flush(400);
    rerender({ email: 'new@x.com' });
    await flush(400);
    expect(result.current.status).toMatchObject({ tone: 'valid', message: 'Email address is available' });
    await act(async () => { resolveOld(true); await Promise.resolve(); });
    expect(result.current.status.tone).toBe('valid'); // the late "taken" for old@x.com is ignored
    expect(result.current.taken).toBe(false);
  });

  it('never reports a failed check as available, and can retry', async () => {
    vi.useFakeTimers();
    const check = vi.fn().mockRejectedValueOnce(new Error('down')).mockResolvedValueOnce(true);
    const { result } = renderHook(() => useEmailAvailability('a@x.com', { check }));
    await flush(400);
    expect(result.current.status.tone).toBe('neutral');
    expect(result.current.failed).toBe(true);
    act(() => result.current.retry());
    expect(result.current.status.tone).toBe('pending');
    await flush(400);
    expect(result.current).toMatchObject({ taken: true, status: { message: 'This email address is already registered' } });
  });
});
