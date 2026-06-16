import axios from 'axios'

let accessToken = null

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  timeout: 10000,
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

export function setAccessToken(token) {
  accessToken = token
}

export async function fetchClasses() {
  const response = await api.get('/classes')
  return response.data
}

export async function fetchPassTypes() {
  const response = await api.get('/pass-types')
  return response.data
}

export async function registerUser(payload) {
  const response = await api.post('/auth/register', payload)
  return response.data
}

export async function verifyEmail(token) {
  await api.post('/auth/verify-email', { token })
}

export async function loginUser(payload) {
  const response = await api.post('/auth/login', payload)
  return response.data
}

export async function refreshSession() {
  const response = await api.post('/auth/refresh')
  return response.data
}

export async function logoutUser() {
  await api.post('/auth/logout')
}

export async function fetchMe() {
  const response = await api.get('/me')
  return response.data
}

export async function updateMyProfile(payload) {
  const response = await api.patch('/me', payload)
  return response.data
}

export async function fetchMyPasses() {
  const response = await api.get('/me/passes')
  return response.data
}

export async function fetchMyPassOrders() {
  const response = await api.get('/me/pass-orders')
  return response.data
}

export async function createMyPassOrder(passTypeId) {
  const response = await api.post('/me/pass-orders', { passTypeId })
  return response.data
}

export async function cancelMyPassOrder(id) {
  await api.delete(`/me/pass-orders/${id}`)
}

export async function fetchMyNotifications() {
  const response = await api.get('/me/notifications')
  return response.data
}

export async function markNotificationRead(id) {
  const response = await api.post(`/me/notifications/${id}/read`)
  return response.data
}

export async function fetchMyReservations() {
  const response = await api.get('/me/reservations')
  return response.data
}

export async function fetchMyWaitlist() {
  const response = await api.get('/me/waitlist')
  return response.data
}

export async function bookClass(classId) {
  const response = await api.post(`/classes/${classId}/reservations`)
  return response.data
}

export async function cancelMyReservation(classId) {
  await api.delete(`/classes/${classId}/reservations/me`)
}

export async function leaveMyWaitlist(classId) {
  await api.delete(`/classes/${classId}/waitlist/me`)
}

export async function fetchAdminUsers() {
  const response = await api.get('/admin/users')
  return response.data
}

export async function changeUserRole(id, role) {
  const response = await api.patch(`/admin/users/${id}/role`, { role })
  return response.data
}

export async function changeUserStatus(id, status) {
  const response = await api.patch(`/admin/users/${id}/status`, { status })
  return response.data
}

export async function fetchInstructors(admin = false) {
  const response = await api.get(admin ? '/admin/instructors' : '/instructors')
  return response.data
}

export async function createInstructor(payload) {
  const response = await api.post('/admin/instructors', payload)
  return response.data
}

export async function updateInstructor(id, payload) {
  const response = await api.patch(`/admin/instructors/${id}`, payload)
  return response.data
}

export async function fetchAdminClasses() {
  const response = await api.get('/admin/classes')
  return response.data
}

export async function createClassSession(payload) {
  const response = await api.post('/admin/classes', payload)
  return response.data
}

export async function updateClassSession(id, payload) {
  const response = await api.patch(`/admin/classes/${id}`, payload)
  return response.data
}

export async function publishClassSession(id) {
  const response = await api.post(`/admin/classes/${id}/publish`)
  return response.data
}

export async function cancelClassSession(id) {
  const response = await api.post(`/admin/classes/${id}/cancel`)
  return response.data
}

export async function fetchAdminPassTypes() {
  const response = await api.get('/admin/pass-types')
  return response.data
}

export async function createPassType(payload) {
  const response = await api.post('/admin/pass-types', payload)
  return response.data
}

export async function updatePassType(id, payload) {
  const response = await api.patch(`/admin/pass-types/${id}`, payload)
  return response.data
}

export async function grantUserPass(payload) {
  const response = await api.post('/admin/user-passes', payload)
  return response.data
}

export async function fetchAdminPassOrders() {
  const response = await api.get('/admin/pass-orders')
  return response.data
}

export async function completePassOrderPayment(id, paymentReference = '') {
  const response = await api.post(`/admin/pass-orders/${id}/pay`, { paymentReference })
  return response.data
}

export async function cancelAdminPassOrder(id) {
  const response = await api.post(`/admin/pass-orders/${id}/cancel`)
  return response.data
}

export function getApiError(error, fallback) {
  return error.response?.data?.message || fallback
}
