/**
 * index.js — Page controller for the Flight List page (index.html).
 *
 * Responsibilities:
 * - Initial load: fetch and render flights with default pagination
 * - Filter form: filter by origin, destination, status
 * - Create flight modal: submit and refresh list
 * - Delete action per row: confirm → delete → refresh
 * - Row / View link: navigate to flight-detail.html?id={id}
 * - Pagination: prev / page numbers / next
 */

import { fetchFlights, createFlight, deleteFlight } from './api.js';
import {
  showToast,
  showSpinner,
  hideSpinner,
  showModal,
  hideModal,
  showConfirm,
  renderStatusBadge,
  formatDateTime,
} from './ui.js';

// -----------------------------------------------------------------------
// Module-level state
// -----------------------------------------------------------------------
const state = {
  page: 0,
  size: 20,
  origin: '',
  destination: '',
  status: '',
  totalPages: 0,
};

// -----------------------------------------------------------------------
// Initialisation
// -----------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
  bindFilterForm();
  bindCreateButton();
  bindCreateForm();
  bindModalCloseOnBackdrop();
  loadFlights();
});

// -----------------------------------------------------------------------
// Data loading & rendering
// -----------------------------------------------------------------------
async function loadFlights() {
  showSpinner();
  try {
    const data = await fetchFlights({
      origin: state.origin,
      destination: state.destination,
      status: state.status,
      page: state.page,
      size: state.size,
    });

    state.totalPages = data.total_pages ?? 0;

    renderTable(data.items ?? []);
    renderPagination(data);
  } catch (err) {
    showToast(err.detail || 'Failed to load flights.', 'error');
  } finally {
    hideSpinner();
  }
}

// -----------------------------------------------------------------------
// Table rendering
// -----------------------------------------------------------------------
function renderTable(flights) {
  const tbody = document.getElementById('flights-body');
  const noFlights = document.getElementById('no-flights');

  tbody.innerHTML = '';

  if (flights.length === 0) {
    noFlights.hidden = false;
    return;
  }

  noFlights.hidden = true;

  flights.forEach((flight) => {
    const tr = document.createElement('tr');
    tr.dataset.id = flight.id;

    // Navigate to detail on row click (but not when clicking action buttons)
    tr.addEventListener('click', (e) => {
      if (e.target.closest('.btn')) return;
      navigateToDetail(flight.id);
    });

    tr.innerHTML = `
      <td>${escapeHtml(flight.flight_number)}</td>
      <td>${escapeHtml(flight.airline)}</td>
      <td>${escapeHtml(flight.origin)} → ${escapeHtml(flight.destination)}</td>
      <td>${escapeHtml(formatDateTime(flight.departure_time))}</td>
      <td>${escapeHtml(formatDateTime(flight.arrival_time))}</td>
      <td class="status-cell"></td>
      <td class="actions-cell">
        <button class="btn btn-secondary btn-sm view-btn" data-id="${escapeAttr(flight.id)}">View</button>
        <button class="btn btn-danger btn-sm delete-btn" data-id="${escapeAttr(flight.id)}">Delete</button>
      </td>
    `;

    // Inject status badge (safe — no innerHTML for user data)
    const statusCell = tr.querySelector('.status-cell');
    statusCell.appendChild(renderStatusBadge(flight.status));

    // View button
    tr.querySelector('.view-btn').addEventListener('click', (e) => {
      e.stopPropagation();
      navigateToDetail(flight.id);
    });

    // Delete button
    tr.querySelector('.delete-btn').addEventListener('click', async (e) => {
      e.stopPropagation();
      await handleDelete(flight.id);
    });

    tbody.appendChild(tr);
  });
}

// -----------------------------------------------------------------------
// Pagination rendering
// -----------------------------------------------------------------------
function renderPagination(data) {
  const container = document.getElementById('pagination');
  container.innerHTML = '';

  const totalPages = data.total_pages ?? 0;
  const currentPage = data.page ?? 0;

  if (totalPages <= 1) return;

  // Prev button
  const prevBtn = createPageButton('‹ Prev', currentPage === 0);
  prevBtn.addEventListener('click', () => {
    state.page = currentPage - 1;
    loadFlights();
  });
  container.appendChild(prevBtn);

  // Page number buttons (show a sliding window of up to 7 pages)
  const pageWindow = buildPageWindow(currentPage, totalPages);
  pageWindow.forEach(({ label, pageIndex, isEllipsis }) => {
    if (isEllipsis) {
      const span = document.createElement('span');
      span.textContent = '…';
      span.style.padding = '0 0.35rem';
      container.appendChild(span);
      return;
    }
    const btn = createPageButton(String(label), false);
    if (pageIndex === currentPage) btn.classList.add('active');
    btn.addEventListener('click', () => {
      state.page = pageIndex;
      loadFlights();
    });
    container.appendChild(btn);
  });

  // Next button
  const nextBtn = createPageButton('Next ›', currentPage >= totalPages - 1);
  nextBtn.addEventListener('click', () => {
    state.page = currentPage + 1;
    loadFlights();
  });
  container.appendChild(nextBtn);
}

function createPageButton(label, disabled) {
  const btn = document.createElement('button');
  btn.className = 'page-btn';
  btn.textContent = label;
  btn.disabled = disabled;
  return btn;
}

/**
 * Builds a display window of page entries. At most 7 slots:
 * first page, last page, current page ±2, and ellipsis placeholders.
 */
function buildPageWindow(current, total) {
  const pages = [];
  const delta = 2;
  const range = new Set([
    0,
    total - 1,
    ...Array.from({ length: delta * 2 + 1 }, (_, i) => current - delta + i),
  ]);

  let prev = -1;
  Array.from(range)
    .filter((p) => p >= 0 && p < total)
    .sort((a, b) => a - b)
    .forEach((p) => {
      if (prev !== -1 && p - prev > 1) {
        pages.push({ label: '…', pageIndex: -1, isEllipsis: true });
      }
      pages.push({ label: p + 1, pageIndex: p, isEllipsis: false });
      prev = p;
    });

  return pages;
}

// -----------------------------------------------------------------------
// Filter form
// -----------------------------------------------------------------------
function bindFilterForm() {
  const form = document.getElementById('filter-form');
  if (!form) return;

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    state.origin = document.getElementById('filter-origin').value.trim().toUpperCase();
    state.destination = document.getElementById('filter-destination').value.trim().toUpperCase();
    state.status = document.getElementById('filter-status').value;
    state.page = 0;
    loadFlights();
  });

  const clearBtn = document.getElementById('filter-clear-btn');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      form.reset();
      state.origin = '';
      state.destination = '';
      state.status = '';
      state.page = 0;
      loadFlights();
    });
  }
}

// -----------------------------------------------------------------------
// Create Flight modal
// -----------------------------------------------------------------------
function bindCreateButton() {
  const btn = document.getElementById('create-btn');
  if (btn) {
    btn.addEventListener('click', () => {
      resetCreateForm();
      showModal('create-modal');
    });
  }
}

function bindCreateForm() {
  const form = document.getElementById('create-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!validateCreateForm(form)) return;

    const data = {
      flight_number: form.elements['flight_number'].value.trim(),
      airline: form.elements['airline'].value.trim(),
      origin: form.elements['origin'].value.trim().toUpperCase(),
      destination: form.elements['destination'].value.trim().toUpperCase(),
      departure_time: toIsoOffsetString(form.elements['departure_time'].value),
      arrival_time: toIsoOffsetString(form.elements['arrival_time'].value),
    };

    showSpinner();
    try {
      await createFlight(data);
      hideModal('create-modal');
      showToast('Flight created successfully.', 'success');
      state.page = 0;
      await loadFlights();
    } catch (err) {
      showToast(err.detail || 'Failed to create flight.', 'error');
    } finally {
      hideSpinner();
    }
  });

  const cancelBtn = document.getElementById('create-cancel-btn');
  if (cancelBtn) {
    cancelBtn.addEventListener('click', () => hideModal('create-modal'));
  }
}

function resetCreateForm() {
  const form = document.getElementById('create-form');
  if (form) {
    form.reset();
    form.querySelectorAll('.field-error').forEach((el) => (el.textContent = ''));
    form.querySelectorAll('.input-error').forEach((el) => el.classList.remove('input-error'));
  }
}

/**
 * Client-side validation for the create flight form.
 * Returns true if all fields pass, false otherwise.
 */
function validateCreateForm(form) {
  let valid = true;

  const rules = [
    {
      name: 'flight_number',
      errorId: 'err-flight-number',
      validate: (v) => /^[A-Z]{2}\d{3,4}$/.test(v.trim()),
      message: 'Flight number must match pattern AA123 or AA1234 (uppercase letters + 3-4 digits).',
    },
    {
      name: 'airline',
      errorId: 'err-airline',
      validate: (v) => v.trim().length >= 1 && v.trim().length <= 100,
      message: 'Airline is required (max 100 characters).',
    },
    {
      name: 'origin',
      errorId: 'err-origin',
      validate: (v) => /^[A-Z]{3}$/.test(v.trim().toUpperCase()),
      message: 'Origin must be a 3-letter IATA code (e.g. JFK).',
    },
    {
      name: 'destination',
      errorId: 'err-destination',
      validate: (v) => /^[A-Z]{3}$/.test(v.trim().toUpperCase()),
      message: 'Destination must be a 3-letter IATA code (e.g. LAX).',
    },
    {
      name: 'departure_time',
      errorId: 'err-departure-time',
      validate: (v) => Boolean(v),
      message: 'Departure time is required.',
    },
    {
      name: 'arrival_time',
      errorId: 'err-arrival-time',
      validate: (v) => Boolean(v),
      message: 'Arrival time is required.',
    },
  ];

  rules.forEach(({ name, errorId, validate, message }) => {
    const input = form.elements[name];
    const errorEl = document.getElementById(errorId);
    const value = input ? input.value : '';
    if (!validate(value)) {
      if (errorEl) errorEl.textContent = message;
      if (input) input.classList.add('input-error');
      valid = false;
    } else {
      if (errorEl) errorEl.textContent = '';
      if (input) input.classList.remove('input-error');
    }
  });

  // Cross-field: origin ≠ destination
  const origin = form.elements['origin'].value.trim().toUpperCase();
  const destination = form.elements['destination'].value.trim().toUpperCase();
  if (origin && destination && origin === destination) {
    const errEl = document.getElementById('err-destination');
    if (errEl) errEl.textContent = 'Destination must differ from origin.';
    form.elements['destination'].classList.add('input-error');
    valid = false;
  }

  // Cross-field: arrival > departure
  const dep = form.elements['departure_time'].value;
  const arr = form.elements['arrival_time'].value;
  if (dep && arr && new Date(arr) <= new Date(dep)) {
    const errEl = document.getElementById('err-arrival-time');
    if (errEl) errEl.textContent = 'Arrival time must be after departure time.';
    form.elements['arrival_time'].classList.add('input-error');
    valid = false;
  }

  return valid;
}

// -----------------------------------------------------------------------
// Delete handler
// -----------------------------------------------------------------------
async function handleDelete(id) {
  const confirmed = await showConfirm('Delete this flight?');
  if (!confirmed) return;

  showSpinner();
  try {
    await deleteFlight(id);
    showToast('Flight deleted.', 'success');
    await loadFlights();
  } catch (err) {
    showToast(err.detail || 'Failed to delete flight.', 'error');
  } finally {
    hideSpinner();
  }
}

// -----------------------------------------------------------------------
// Navigation
// -----------------------------------------------------------------------
function navigateToDetail(id) {
  window.location.href = `flight-detail.html?id=${encodeURIComponent(id)}`;
}

// -----------------------------------------------------------------------
// Modal backdrop click-to-close
// -----------------------------------------------------------------------
function bindModalCloseOnBackdrop() {
  ['create-modal'].forEach((id) => {
    const modal = document.getElementById(id);
    if (!modal) return;
    modal.addEventListener('click', (e) => {
      if (e.target === modal) hideModal(id);
    });
  });
}

// -----------------------------------------------------------------------
// Utilities
// -----------------------------------------------------------------------

/**
 * Converts a datetime-local input value (e.g. "2026-05-07T14:30")
 * to an ISO 8601 offset string with the local timezone offset.
 */
function toIsoOffsetString(localDateTimeValue) {
  if (!localDateTimeValue) return null;
  const date = new Date(localDateTimeValue);
  // toISOString returns UTC; we need to preserve local offset
  const tzOffset = -date.getTimezoneOffset();
  const sign = tzOffset >= 0 ? '+' : '-';
  const pad = (n) => String(Math.abs(n)).padStart(2, '0');
  const hh = pad(Math.floor(Math.abs(tzOffset) / 60));
  const mm = pad(Math.abs(tzOffset) % 60);
  // Format: yyyy-MM-ddTHH:mm:ss+HH:mm
  const iso = date.toISOString().replace('Z', '');
  return `${iso}${sign}${hh}:${mm}`;
}

/** Escapes a value for safe insertion into HTML text content via innerHTML. */
function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/** Escapes a value for safe use in an HTML attribute. */
function escapeAttr(str) {
  if (str == null) return '';
  return String(str).replace(/"/g, '&quot;');
}
