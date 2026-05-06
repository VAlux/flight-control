/**
 * ui.js — DOM helper utilities for Flight Control.
 *
 * All functions operate on IDs/elements defined in index.html.
 * No direct API calls are made here.
 */

const TOAST_DURATION_MS = 4000;
const TOAST_ANIMATION_MS = 250;

/**
 * Creates and appends a toast notification to #toast-container.
 * The toast auto-removes after TOAST_DURATION_MS milliseconds.
 *
 * @param {string} message       - Text to display
 * @param {'success'|'error'} type - Visual variant
 */
export function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.setAttribute('role', 'status');
  toast.setAttribute('aria-live', 'polite');

  const icon = document.createElement('span');
  icon.textContent = type === 'success' ? '✓' : '✕';
  icon.setAttribute('aria-hidden', 'true');

  const text = document.createElement('span');
  text.textContent = message;

  toast.appendChild(icon);
  toast.appendChild(text);
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('toast-hiding');
    setTimeout(() => toast.remove(), TOAST_ANIMATION_MS);
  }, TOAST_DURATION_MS);
}

/**
 * Shows the loading spinner overlay (#spinner).
 */
export function showSpinner() {
  const spinner = document.getElementById('spinner');
  if (spinner) spinner.hidden = false;
}

/**
 * Hides the loading spinner overlay (#spinner).
 */
export function hideSpinner() {
  const spinner = document.getElementById('spinner');
  if (spinner) spinner.hidden = true;
}

/**
 * Makes a modal element visible by removing the hidden attribute.
 *
 * @param {string} modalId - ID of the modal element
 */
export function showModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.hidden = false;
    // Trap focus: move focus to the first focusable element inside the card
    const focusable = modal.querySelector('input, select, button, textarea, [tabindex]');
    if (focusable) focusable.focus();
  }
}

/**
 * Hides a modal element by setting the hidden attribute.
 *
 * @param {string} modalId - ID of the modal element
 */
export function hideModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) modal.hidden = true;
}

/**
 * Shows a confirmation dialog modal and returns a Promise<boolean>.
 * Resolves true if the user confirms, false if they cancel.
 *
 * @param {string} message - Prompt text shown to the user
 * @returns {Promise<boolean>}
 */
export function showConfirm(message) {
  return new Promise((resolve) => {
    const modal = document.getElementById('confirm-modal');
    const messageEl = document.getElementById('confirm-message');
    const confirmBtn = document.getElementById('confirm-ok-btn');
    const cancelBtn = document.getElementById('confirm-cancel-btn');

    if (!modal || !confirmBtn || !cancelBtn) {
      // Fallback to native confirm if the modal structure is missing
      resolve(window.confirm(message));
      return;
    }

    if (messageEl) messageEl.textContent = message;

    modal.hidden = false;
    confirmBtn.focus();

    function handleConfirm() {
      cleanup();
      resolve(true);
    }

    function handleCancel() {
      cleanup();
      resolve(false);
    }

    function handleKeydown(event) {
      if (event.key === 'Escape') {
        handleCancel();
      }
    }

    function cleanup() {
      modal.hidden = true;
      confirmBtn.removeEventListener('click', handleConfirm);
      cancelBtn.removeEventListener('click', handleCancel);
      document.removeEventListener('keydown', handleKeydown);
    }

    confirmBtn.addEventListener('click', handleConfirm);
    cancelBtn.addEventListener('click', handleCancel);
    document.addEventListener('keydown', handleKeydown);
  });
}

/**
 * Creates a status badge <span> element for the given FlightStatus.
 *
 * @param {string} status - FlightStatus enum string (e.g. 'SCHEDULED')
 * @returns {HTMLSpanElement}
 */
export function renderStatusBadge(status) {
  const span = document.createElement('span');
  span.className = `badge badge-${status}`;
  span.textContent = status.replace('_', ' ');
  return span;
}

/**
 * Formats an ISO 8601 date-time string into a human-readable locale string.
 * Example output: "May 7, 2026, 2:30 PM"
 *
 * @param {string} isoString - ISO 8601 date-time string
 * @returns {string}
 */
export function formatDateTime(isoString) {
  if (!isoString) return '—';
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return isoString;
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}
