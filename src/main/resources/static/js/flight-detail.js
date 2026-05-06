/**
 * flight-detail.js — Page controller for flight-detail.html.
 *
 * Reads ?id= from the URL, fetches the flight, and renders the full
 * detail view including status transition buttons, edit form, and
 * delete action.
 *
 * XSS safety: all user-derived data is written via textContent or
 * DOM property assignment. innerHTML is used only for trusted static
 * markup (the status badge span produced by renderStatusBadge).
 */

import { getFlight, updateFlight, deleteFlight, transitionStatus } from './api.js';
import {
  showToast,
  showSpinner,
  hideSpinner,
  showConfirm,
  renderStatusBadge,
  formatDateTime,
} from './ui.js';

// ---------------------------------------------------------------------------
// Client-side mirror of the backend FlightStateMachine.
// Kept in sync with FlightStatus transitions enforced on the server.
// The server is the ground truth — a 409 from the backend will surface
// as an error toast even if the button should not have been shown.
// ---------------------------------------------------------------------------
const ALLOWED_TRANSITIONS = {
  SCHEDULED: ['DELAYED', 'DEPARTED', 'CANCELLED'],
  DELAYED:   ['DEPARTED', 'CANCELLED'],
  DEPARTED:  ['IN_AIR'],
  IN_AIR:    ['LANDED'],
  LANDED:    [],
  CANCELLED: [],
};

// Statuses for which the edit form is available
const EDITABLE_STATUSES = new Set(['SCHEDULED', 'DELAYED']);

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------
let flightId = null;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Converts a datetime-local input value (YYYY-MM-DDThh:mm) to an
 * OffsetDateTime ISO string accepted by the backend.
 * Appends ":00Z" when seconds and offset are absent so the backend
 * parser receives a valid ISO-8601 value.
 *
 * @param {string} value - datetime-local input value
 * @returns {string} ISO 8601 offset date-time string
 */
function toOffsetDateTime(value) {
  if (!value) return value;
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
    const date = new Date(value);
    const offsetMin = -date.getTimezoneOffset();
    const sign = offsetMin >= 0 ? '+' : '-';
    const abs = Math.abs(offsetMin);
    const hh = String(Math.floor(abs / 60)).padStart(2, '0');
    const mm = String(abs % 60).padStart(2, '0');
    return `${value}:00${sign}${hh}:${mm}`;
  }
  return value;
}

/**
 * Formats an OffsetDateTime ISO string to the value expected by an
 * <input type="datetime-local"> (YYYY-MM-DDThh:mm).
 *
 * @param {string} isoString
 * @returns {string}
 */
function toDatetimeLocalValue(isoString) {
  if (!isoString) return '';
  // Take the first 16 characters: "YYYY-MM-DDThh:mm"
  return isoString.substring(0, 16);
}

// ---------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------

/**
 * Populates the #flight-info <dl> with all flight fields.
 * Also updates the document title.
 *
 * @param {object} flight - FlightResponse object
 */
function renderDetail(flight) {
  document.title = `Flight ${flight.flight_number} — Flight Control`;

  setText('info-id', flight.id);
  setText('info-flight-number', flight.flight_number);
  setText('info-airline', flight.airline);

  const routeEl = document.getElementById('info-route');
  if (routeEl) {
    routeEl.textContent = '';
    routeEl.textContent = `${flight.origin} → ${flight.destination}`;
  }

  setText('info-departure-time', formatDateTime(flight.departure_time));
  setText('info-arrival-time', formatDateTime(flight.arrival_time));

  const statusEl = document.getElementById('info-status');
  if (statusEl) {
    statusEl.textContent = '';
    statusEl.appendChild(renderStatusBadge(flight.status));
  }

  setText('info-created-at', formatDateTime(flight.created_at));
  setText('info-updated-at', formatDateTime(flight.updated_at));
}

/**
 * Sets textContent on the element with the given id, safely.
 *
 * @param {string} id
 * @param {string} value
 */
function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value ?? '—';
}

/**
 * Renders one button per allowed next state in #transition-buttons.
 * If the current status is terminal (no allowed transitions) hides
 * the buttons container and shows #terminal-msg.
 *
 * @param {object} flight - FlightResponse object
 */
function renderTransitions(flight) {
  const buttonsDiv = document.getElementById('transition-buttons');
  const terminalMsg = document.getElementById('terminal-msg');

  if (!buttonsDiv) return;

  // Clear previous buttons
  buttonsDiv.textContent = '';

  const nextStatuses = ALLOWED_TRANSITIONS[flight.status] ?? [];

  if (nextStatuses.length === 0) {
    buttonsDiv.hidden = true;
    if (terminalMsg) terminalMsg.hidden = false;
    return;
  }

  buttonsDiv.hidden = false;
  if (terminalMsg) terminalMsg.hidden = true;

  nextStatuses.forEach((nextStatus) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = 'btn btn-transition';
    btn.textContent = nextStatus.replaceAll('_', ' ');
    btn.addEventListener('click', () => handleTransition(nextStatus));
    buttonsDiv.appendChild(btn);
  });
}

/**
 * Shows or hides the edit toggle button and delete button based on
 * the current flight status.
 *
 * @param {object} flight - FlightResponse object
 */
function renderEditability(flight) {
  const toggleEditBtn = document.getElementById('toggle-edit-btn');
  const deleteBtn = document.getElementById('delete-btn');

  if (toggleEditBtn) {
    toggleEditBtn.hidden = !EDITABLE_STATUSES.has(flight.status);
  }

  if (deleteBtn) {
    deleteBtn.hidden = flight.status !== 'SCHEDULED';
  }
}

/**
 * Pre-populates the #edit-form inputs with the current flight values.
 *
 * @param {object} flight - FlightResponse object
 */
function populateEditForm(flight) {
  setInputValue('edit-flight-number', flight.flight_number);
  setInputValue('edit-airline', flight.airline);
  setInputValue('edit-origin', flight.origin);
  setInputValue('edit-destination', flight.destination);
  setInputValue('edit-departure-time', toDatetimeLocalValue(flight.departure_time));
  setInputValue('edit-arrival-time', toDatetimeLocalValue(flight.arrival_time));
}

/**
 * Sets the value of an input element by id.
 *
 * @param {string} id
 * @param {string} value
 */
function setInputValue(id, value) {
  const el = document.getElementById(id);
  if (el) el.value = value ?? '';
}

/**
 * Renders the full detail card from a FlightResponse.
 * Called on initial load and after every successful mutation.
 *
 * @param {object} flight - FlightResponse object
 */
function renderAll(flight) {
  renderDetail(flight);
  renderTransitions(flight);
  renderEditability(flight);
  populateEditForm(flight);

  // Ensure the detail card is visible and loading/error messages are hidden
  setHidden('loading-msg', true);
  setHidden('error-msg', true);
  setHidden('detail-card', false);
}

/**
 * Sets the hidden attribute on an element by id.
 *
 * @param {string} id
 * @param {boolean} hidden
 */
function setHidden(id, hidden) {
  const el = document.getElementById(id);
  if (el) el.hidden = hidden;
}

/**
 * Displays the error message element with the given text.
 *
 * @param {string} message
 */
function showError(message) {
  setHidden('loading-msg', true);
  setHidden('detail-card', true);

  const errorEl = document.getElementById('error-msg');
  if (errorEl) {
    errorEl.textContent = message;
    errorEl.hidden = false;
  }
}

// ---------------------------------------------------------------------------
// Event handlers
// ---------------------------------------------------------------------------

/**
 * Handles a status transition button click.
 *
 * @param {string} newStatus - Target FlightStatus
 */
async function handleTransition(newStatus) {
  showSpinner();
  try {
    const updated = await transitionStatus(flightId, newStatus);
    renderAll(updated);
    showToast(`Status updated to ${newStatus.replaceAll('_', ' ')}`, 'success');
  } catch (err) {
    showToast(err.detail || 'Failed to update status.', 'error');
  } finally {
    hideSpinner();
  }
}

/**
 * Handles the edit form submission.
 *
 * @param {Event} event
 */
async function handleEditSubmit(event) {
  event.preventDefault();

  const form = document.getElementById('edit-form');
  if (!form) return;

  // Clear previous field errors
  clearFieldErrors();

  const flightNumber = document.getElementById('edit-flight-number')?.value.trim().toUpperCase();
  const airline = document.getElementById('edit-airline')?.value.trim();
  const origin = document.getElementById('edit-origin')?.value.trim().toUpperCase();
  const destination = document.getElementById('edit-destination')?.value.trim().toUpperCase();
  const departureRaw = document.getElementById('edit-departure-time')?.value;
  const arrivalRaw = document.getElementById('edit-arrival-time')?.value;

  // Client-side validation
  let hasError = false;

  if (!flightNumber || !/^[A-Z]{2}\d{3,4}$/.test(flightNumber)) {
    setFieldError('edit-err-flight-number', 'Flight number must be 2 uppercase letters followed by 3-4 digits (e.g. AA123).');
    hasError = true;
  }
  if (!airline) {
    setFieldError('edit-err-airline', 'Airline is required.');
    hasError = true;
  }
  if (!origin || !/^[A-Z]{3}$/.test(origin)) {
    setFieldError('edit-err-origin', 'Origin must be 3 uppercase letters (IATA code).');
    hasError = true;
  }
  if (!destination || !/^[A-Z]{3}$/.test(destination)) {
    setFieldError('edit-err-destination', 'Destination must be 3 uppercase letters (IATA code).');
    hasError = true;
  }
  if (!departureRaw) {
    setFieldError('edit-err-departure-time', 'Departure time is required.');
    hasError = true;
  }
  if (!arrivalRaw) {
    setFieldError('edit-err-arrival-time', 'Arrival time is required.');
    hasError = true;
  }
  if (!hasError && origin === destination) {
    setFieldError('edit-err-destination', 'Destination must differ from origin.');
    hasError = true;
  }

  if (hasError) return;

  const data = {
    flight_number: flightNumber,
    airline,
    origin,
    destination,
    departure_time: toOffsetDateTime(departureRaw),
    arrival_time: toOffsetDateTime(arrivalRaw),
  };

  showSpinner();
  try {
    const updated = await updateFlight(flightId, data);
    // Hide the edit form and show the edit button again
    setHidden('edit-section', true);
    setHidden('toggle-edit-btn', false);
    renderAll(updated);
    showToast('Flight updated successfully.', 'success');
  } catch (err) {
    showToast(err.detail || 'Failed to update flight.', 'error');
  } finally {
    hideSpinner();
  }
}

/**
 * Handles the delete button click.
 */
async function handleDelete() {
  const confirmed = await showConfirm('Delete this flight? This action cannot be undone.');
  if (!confirmed) return;

  showSpinner();
  try {
    await deleteFlight(flightId);
    // Queue a success toast for the list page via sessionStorage and navigate
    sessionStorage.setItem('pendingToast', JSON.stringify({
      message: 'Flight deleted successfully.',
      type: 'success',
    }));
    window.location.href = 'index.html';
  } catch (err) {
    hideSpinner();
    showToast(err.detail || 'Failed to delete flight.', 'error');
  }
}

// ---------------------------------------------------------------------------
// Field error helpers
// ---------------------------------------------------------------------------

/**
 * Clears all edit-form field error messages.
 */
function clearFieldErrors() {
  const errorIds = [
    'edit-err-flight-number',
    'edit-err-airline',
    'edit-err-origin',
    'edit-err-destination',
    'edit-err-departure-time',
    'edit-err-arrival-time',
  ];
  errorIds.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.textContent = '';
  });
}

/**
 * Displays a validation message in a field error element.
 *
 * @param {string} id      - ID of the field-error span
 * @param {string} message - Error text to display
 */
function setFieldError(id, message) {
  const el = document.getElementById(id);
  if (el) el.textContent = message;
}

// ---------------------------------------------------------------------------
// Wire event listeners
// ---------------------------------------------------------------------------

function wireEvents() {
  // Toggle edit section
  document.getElementById('toggle-edit-btn')?.addEventListener('click', () => {
    setHidden('edit-section', false);
    setHidden('toggle-edit-btn', true);
    document.getElementById('edit-flight-number')?.focus();
  });

  // Cancel edit
  document.getElementById('cancel-edit-btn')?.addEventListener('click', () => {
    setHidden('edit-section', true);
    setHidden('toggle-edit-btn', false);
  });

  // Edit form submit
  document.getElementById('edit-form')?.addEventListener('submit', handleEditSubmit);

  // Delete button
  document.getElementById('delete-btn')?.addEventListener('click', handleDelete);
}

// ---------------------------------------------------------------------------
// Initialisation
// ---------------------------------------------------------------------------

async function init() {
  wireEvents();

  const params = new URLSearchParams(window.location.search);
  flightId = params.get('id');

  if (!flightId) {
    showError('No flight ID specified. Please go back and select a flight.');
    return;
  }

  showSpinner();
  try {
    const flight = await getFlight(flightId);
    renderAll(flight);
  } catch (err) {
    const message = err.status === 404
      ? 'Flight not found.'
      : (err.detail || 'Failed to load flight.');
    showError(message);
  } finally {
    hideSpinner();
  }
}

// Run once the DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
