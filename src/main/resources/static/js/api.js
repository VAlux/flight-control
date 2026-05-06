/**
 * api.js — Flight Control API client.
 *
 * All functions return parsed JSON on success.
 * On non-2xx responses they throw an Error with:
 *   error.status  — HTTP status code (number)
 *   error.detail  — human-readable detail from Problem Detail body (string)
 */

const API_BASE = '/api/v1/flights';

/**
 * Builds an ApiError from a non-2xx Response.
 * Attempts to parse a Problem Detail body; falls back to a generic message.
 */
async function buildError(response) {
  let detail = `Request failed with status ${response.status}`;
  try {
    const body = await response.json();
    if (body && body.detail) {
      detail = body.detail;
    }
  } catch (_) {
    // body is not JSON — keep the generic detail message
  }
  const error = new Error(detail);
  error.status = response.status;
  error.detail = detail;
  return error;
}

/**
 * Sends a fetch request and throws a structured ApiError on non-2xx.
 * Returns the Response object so callers can decide how to read the body.
 */
async function apiFetch(url, options = {}) {
  let response;
  try {
    response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers,
      },
      ...options,
    });
  } catch (networkError) {
    const err = new Error('Network error');
    err.status = 0;
    err.detail = 'Network error — could not reach the server.';
    throw err;
  }

  if (!response.ok) {
    throw await buildError(response);
  }

  return response;
}

/**
 * GET /api/v1/flights
 *
 * @param {object} params
 * @param {string} [params.origin]       - 3-letter IATA origin code (optional)
 * @param {string} [params.destination]  - 3-letter IATA destination code (optional)
 * @param {string} [params.status]       - FlightStatus enum value (optional)
 * @param {number} [params.page=0]       - Zero-based page index
 * @param {number} [params.size=20]      - Page size
 * @returns {Promise<{items: object[], page: number, size: number, total_elements: number, total_pages: number}>}
 */
export async function fetchFlights({ origin, destination, status, page = 0, size = 20 } = {}) {
  const params = new URLSearchParams();

  if (origin && origin.trim() !== '') params.append('origin', origin.trim());
  if (destination && destination.trim() !== '') params.append('destination', destination.trim());
  if (status && status.trim() !== '') params.append('status', status.trim());
  params.append('page', String(page));
  params.append('size', String(size));

  const url = `${API_BASE}?${params.toString()}`;
  const response = await apiFetch(url);
  return response.json();
}

/**
 * GET /api/v1/flights/{id}
 *
 * @param {string} id - Flight UUID
 * @returns {Promise<object>} FlightResponse
 */
export async function getFlight(id) {
  const response = await apiFetch(`${API_BASE}/${id}`);
  return response.json();
}

/**
 * POST /api/v1/flights
 *
 * @param {object} data - FlightRequest body (snake_case fields)
 * @returns {Promise<object>} FlightResponse (HTTP 201)
 */
export async function createFlight(data) {
  const response = await apiFetch(API_BASE, {
    method: 'POST',
    body: JSON.stringify(data),
  });
  return response.json();
}

/**
 * PUT /api/v1/flights/{id}
 *
 * @param {string} id   - Flight UUID
 * @param {object} data - FlightRequest body (snake_case fields)
 * @returns {Promise<object>} FlightResponse
 */
export async function updateFlight(id, data) {
  const response = await apiFetch(`${API_BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
  return response.json();
}

/**
 * DELETE /api/v1/flights/{id}
 *
 * @param {string} id - Flight UUID
 * @returns {Promise<void>}
 */
export async function deleteFlight(id) {
  await apiFetch(`${API_BASE}/${id}`, { method: 'DELETE' });
}

/**
 * PATCH /api/v1/flights/{id}/status
 *
 * @param {string} id     - Flight UUID
 * @param {string} status - Target FlightStatus enum value
 * @returns {Promise<object>} FlightResponse
 */
export async function transitionStatus(id, status) {
  const response = await apiFetch(`${API_BASE}/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
  return response.json();
}
