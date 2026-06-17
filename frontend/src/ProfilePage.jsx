import { useCallback, useEffect, useState } from 'react'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Divider,
  Grid,
  Paper,
  Rating,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import {
  cancelMyReservation,
  cancelMyPassOrder,
  createMyPassOrder,
  createMyReview,
  fetchMyNotifications,
  fetchMyPassOrders,
  fetchMyPasses,
  fetchMyReviews,
  fetchPassTypes,
  fetchMyReservations,
  fetchMyWaitlist,
  getApiError,
  leaveMyWaitlist,
  markNotificationRead,
} from './api'
import { useAuth } from './auth-context'

const roleLabels = {
  CLIENT: 'Klient',
  INSTRUCTOR: 'Instruktor',
  ADMIN: 'Administrator',
}

const passStatusLabels = {
  ACTIVE: 'Aktywny',
  EXPIRED: 'Wygasły',
  EXHAUSTED: 'Wykorzystany',
  CANCELLED: 'Anulowany',
}

const orderStatusLabels = {
  PENDING_PAYMENT: 'Oczekuje na płatność',
  PAID: 'Opłacone',
  CANCELLED: 'Anulowane',
}

const replyRoleLabels = {
  INSTRUCTOR: 'Instruktor',
  ADMIN: 'Administrator',
}

function formatDateTime(value) {
  return new Date(value).toLocaleString('pl-PL')
}

function ReviewReplies({ replies }) {
  if (!replies?.length) {
    return null
  }

  return (
    <Stack spacing={1.25} sx={{ mt: 2 }}>
      {replies.map((reply) => (
        <Paper key={reply.id} variant="outlined" sx={{ p: 1.5, bgcolor: 'rgba(179, 136, 103, 0.05)' }}>
          <Typography fontWeight={700}>
            {replyRoleLabels[reply.authorRole] || reply.authorRole}: {reply.authorName}
          </Typography>
          <Typography variant="body2" sx={{ mt: 0.75 }}>{reply.body}</Typography>
          <Typography color="text.secondary" variant="caption" sx={{ mt: 0.75, display: 'block' }}>
            {formatDateTime(reply.createdAt)}
          </Typography>
        </Paper>
      ))}
    </Stack>
  )
}

export default function ProfilePage() {
  const { user, initializing, logout, updateProfile } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '' })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [passes, setPasses] = useState([])
  const [passTypes, setPassTypes] = useState([])
  const [orders, setOrders] = useState([])
  const [notifications, setNotifications] = useState([])
  const [reservations, setReservations] = useState([])
  const [waitlist, setWaitlist] = useState([])
  const [reviews, setReviews] = useState([])
  const [reviewDrafts, setReviewDrafts] = useState({})
  const [bookingLoading, setBookingLoading] = useState(false)

  const loadBookings = useCallback(async () => {
    if (!user || user.role !== 'CLIENT') {
      return
    }
    setBookingLoading(true)
    try {
      const [passData, reservationData, waitlistData] = await Promise.all([
        fetchMyPasses(),
        fetchMyReservations(),
        fetchMyWaitlist(),
      ])
      setPasses(passData)
      setReservations(reservationData)
      setWaitlist(waitlistData)
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się załadować rezerwacji i karnetów'))
    } finally {
      setBookingLoading(false)
    }
  }, [user])

  const loadShop = useCallback(async () => {
    if (!user || user.role !== 'CLIENT') {
      return
    }
    try {
      const [typesData, ordersData, notificationsData] = await Promise.all([
        fetchPassTypes(),
        fetchMyPassOrders(),
        fetchMyNotifications(),
      ])
      setPassTypes(typesData)
      setOrders(ordersData)
      setNotifications(notificationsData)
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się załadować zakupów i powiadomień'))
    }
  }, [user])

  const loadReviews = useCallback(async () => {
    if (!user || user.role !== 'CLIENT') {
      return
    }
    try {
      const reviewData = await fetchMyReviews()
      setReviews(reviewData)
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się załadować opinii'))
    }
  }, [user])

  useEffect(() => {
    if (user) {
      setForm({
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone || '',
      })
    }
  }, [user])

  useEffect(() => {
    loadBookings()
  }, [loadBookings])

  useEffect(() => {
    loadShop()
  }, [loadShop])

  useEffect(() => {
    loadReviews()
  }, [loadReviews])

  if (initializing) {
    return <Box textAlign="center" py={12}><CircularProgress /></Box>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  const initials = `${user.firstName[0]}${user.lastName[0]}`.toUpperCase()

  const save = async (event) => {
    event.preventDefault()
    setError('')
    setMessage('')
    setSaving(true)
    try {
      await updateProfile(form)
      setMessage('Profil zapisany')
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zapisać profilu'))
    } finally {
      setSaving(false)
    }
  }

  const signOut = async () => {
    await logout()
    navigate('/', { replace: true })
  }

  const cancelReservation = async (classId) => {
    setError('')
    setMessage('')
    try {
      await cancelMyReservation(classId)
      setMessage('Rezerwacja anulowana')
      await loadBookings()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się anulować rezerwacji'))
    }
  }

  const leaveWaitlist = async (classId) => {
    setError('')
    setMessage('')
    try {
      await leaveMyWaitlist(classId)
      setMessage('Opuściłeś listę oczekujących')
      await loadBookings()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się opuścić listy oczekujących'))
    }
  }

  const buyPass = async (passTypeId) => {
    setError('')
    setMessage('')
    try {
      await createMyPassOrder(passTypeId)
      setMessage('Zamówienie utworzone. Administrator potwierdzi płatność po jej otrzymaniu.')
      await loadShop()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się utworzyć zamówienia'))
    }
  }

  const cancelOrder = async (id) => {
    setError('')
    setMessage('')
    try {
      await cancelMyPassOrder(id)
      setMessage('Zamówienie anulowane')
      await loadShop()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się anulować zamówienia'))
    }
  }

  const readNotification = async (id) => {
    try {
      const updated = await markNotificationRead(id)
      setNotifications((current) =>
        current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zaktualizować powiadomienia'))
    }
  }

  const changeReviewDraft = (classId, field, value) => {
    setReviewDrafts((current) => ({
      ...current,
      [classId]: {
        rating: current[classId]?.rating || 0,
        comment: current[classId]?.comment || '',
        [field]: value,
      },
    }))
  }

  const createReview = async (classId) => {
    const draft = reviewDrafts[classId] || { rating: 0, comment: '' }
    if (!draft.rating) {
      setError('Wybierz ocenę od 1 do 5.')
      return
    }
    setError('')
    setMessage('')
    try {
      await createMyReview({
        classId,
        rating: draft.rating,
        comment: draft.comment,
      })
      setMessage('Opinia została zapisana.')
      setReviewDrafts((current) => {
        const next = { ...current }
        delete next[classId]
        return next
      })
      await loadReviews()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zapisać opinii'))
    }
  }

  const completedReservations = reservations.filter((item) =>
    ['CONFIRMED', 'ATTENDED'].includes(item.status) && new Date(item.startAt).getTime() < Date.now())
  const reviewedClassIds = new Set(reviews.map((item) => item.classId))
  const pendingReviews = completedReservations.filter((item) => !reviewedClassIds.has(item.classId))

  return (
    <Container maxWidth="md" sx={{ py: 8 }}>
      <Paper elevation={0} variant="outlined" sx={{ p: { xs: 3, md: 5 } }}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3} alignItems="center">
          <Avatar sx={{ width: 88, height: 88, bgcolor: 'primary.main', fontSize: 30 }}>
            {initials}
          </Avatar>
          <Box flex={1} textAlign={{ xs: 'center', sm: 'left' }}>
            <Typography variant="h3">{user.firstName} {user.lastName}</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>{user.email}</Typography>
            <Typography variant="overline" color="primary.main">{roleLabels[user.role] || user.role}</Typography>
          </Box>
          <Button variant="outlined" onClick={signOut}>Wyloguj się</Button>
        </Stack>

        <Box component="form" onSubmit={save} sx={{ mt: 5 }}>
          <Typography variant="h5" sx={{ mb: 2 }}>Dane osobowe</Typography>
          <Stack spacing={2}>
            {message && <Alert severity="success">{message}</Alert>}
            {error && <Alert severity="error">{error}</Alert>}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                fullWidth
                required
                label="Imię"
                value={form.firstName}
                onChange={(event) => setForm({ ...form, firstName: event.target.value })}
              />
              <TextField
                fullWidth
                required
                label="Nazwisko"
                value={form.lastName}
                onChange={(event) => setForm({ ...form, lastName: event.target.value })}
              />
            </Stack>
            <TextField
              label="Telefon"
              value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })}
            />
            <Button type="submit" variant="contained" disabled={saving} sx={{ alignSelf: 'start' }}>
              {saving ? 'Zapisywanie...' : 'Zapisz'}
            </Button>
          </Stack>
        </Box>

        {user.role === 'CLIENT' && (
          <>
            <Divider sx={{ my: 5 }} />
            <Stack spacing={3}>
              <Box>
                <Typography variant="h5">Kup karnet</Typography>
                <Typography color="text.secondary">Utwórz zamówienie, a administrator potwierdzi płatność.</Typography>
              </Box>
              <Grid container spacing={2}>
                {passTypes.map((item) => (
                  <Grid item xs={12} md={4} key={item.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Typography fontWeight={700}>{item.name}</Typography>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>
                          {item.type === 'UNLIMITED' ? 'Bez limitu' : `Limit: ${item.visitCount} wejść`} · {item.validityDays} dni
                        </Typography>
                        <Typography variant="h6" sx={{ mt: 1 }}>{item.price} {item.currency}</Typography>
                        <Button fullWidth variant="contained" sx={{ mt: 2 }} onClick={() => buyPass(item.id)}>
                          Zamów
                        </Button>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>

              <Box>
                <Typography variant="h5">Moje zamówienia</Typography>
              </Box>
              <Stack spacing={1.5}>
                {orders.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2 }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.passName}</Typography>
                        <Typography color="text.secondary">
                          {item.amount} {item.currency} · {orderStatusLabels[item.status] || item.status}
                        </Typography>
                        <Typography variant="body2">
                          Utworzono {new Date(item.createdAt).toLocaleString('pl-PL')}
                        </Typography>
                      </Box>
                      {item.status === 'PENDING_PAYMENT' && (
                        <Button color="error" onClick={() => cancelOrder(item.id)}>
                          Anuluj zamówienie
                        </Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {orders.length === 0 && <Typography color="text.secondary">Brak zamówień.</Typography>}
              </Stack>

              <Box>
                <Typography variant="h5">Moje karnety</Typography>
                <Typography color="text.secondary">Aktywne i wykorzystane pakiety zajęć</Typography>
              </Box>
              {bookingLoading && <CircularProgress size={24} />}
              <Grid container spacing={2}>
                {passes.map((item) => (
                  <Grid item xs={12} md={4} key={item.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Stack direction="row" justifyContent="space-between" gap={1}>
                          <Typography fontWeight={700}>{item.passName}</Typography>
                          <Chip size="small" label={passStatusLabels[item.status] || item.status} />
                        </Stack>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>
                          {item.type === 'UNLIMITED' ? 'Bez limitu' : `Pozostało wejść: ${item.remainingVisits}`}
                        </Typography>
                        <Typography variant="body2" sx={{ mt: 1 }}>
                          Ważny do {new Date(item.validUntil).toLocaleDateString('pl-PL')}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
                {!bookingLoading && passes.length === 0 && (
                  <Grid item xs={12}><Typography color="text.secondary">Brak karnetów.</Typography></Grid>
                )}
              </Grid>

              <Box>
                <Typography variant="h5">Moje rezerwacje</Typography>
                <Typography color="text.secondary">Anulowanie później niż 12 godzin przed zajęciami jest traktowane jako późne i wejście przepada.</Typography>
              </Box>
              <Stack spacing={1.5}>
                {reservations.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2 }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.classTitle}</Typography>
                        <Typography color="text.secondary">
                          {new Date(item.startAt).toLocaleString('pl-PL')} · {item.instructorName}
                        </Typography>
                        <Typography variant="body2">Karnet: {item.passName || 'nie podano'}</Typography>
                      </Box>
                      {item.status === 'CONFIRMED' && (
                        <Button color="error" onClick={() => cancelReservation(item.classId)}>
                          Anuluj
                        </Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {!bookingLoading && reservations.length === 0 && (
                  <Typography color="text.secondary">Brak aktywnych rezerwacji.</Typography>
                )}
              </Stack>

              <Box>
                <Typography variant="h5">Opinie o instruktorach</Typography>
                <Typography color="text.secondary">Po zakończonych zajęciach możesz ocenić instruktora i dodać komentarz.</Typography>
              </Box>
              <Stack spacing={2}>
                {pendingReviews.map((item) => {
                  const draft = reviewDrafts[item.classId] || { rating: 0, comment: '' }
                  return (
                    <Paper variant="outlined" sx={{ p: 2.5 }} key={`review-form-${item.classId}`}>
                      <Typography fontWeight={700}>{item.classTitle}</Typography>
                      <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        {formatDateTime(item.startAt)} · {item.instructorName}
                      </Typography>
                      <Stack spacing={2} sx={{ mt: 2 }}>
                        <Box>
                          <Typography variant="body2" sx={{ mb: 0.75 }}>Twoja ocena</Typography>
                          <Rating
                            value={draft.rating}
                            onChange={(_, value) => changeReviewDraft(item.classId, 'rating', value || 0)}
                          />
                        </Box>
                        <TextField
                          multiline
                          minRows={3}
                          label="Komentarz"
                          value={draft.comment}
                          onChange={(event) => changeReviewDraft(item.classId, 'comment', event.target.value)}
                        />
                        <Button variant="contained" sx={{ alignSelf: 'start' }} onClick={() => createReview(item.classId)}>
                          Dodaj opinię
                        </Button>
                      </Stack>
                    </Paper>
                  )
                })}

                {reviews.map((review) => (
                  <Paper variant="outlined" sx={{ p: 2.5 }} key={review.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{review.classTitle}</Typography>
                        <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                          Instruktor: {review.instructorName}
                        </Typography>
                      </Box>
                      <Box textAlign={{ sm: 'right' }}>
                        <Rating value={review.rating} readOnly />
                        <Typography color="text.secondary" variant="body2">
                          {formatDateTime(review.createdAt)}
                        </Typography>
                      </Box>
                    </Stack>
                    {review.comment && (
                      <Typography sx={{ mt: 1.5 }}>{review.comment}</Typography>
                    )}
                    <ReviewReplies replies={review.replies} />
                  </Paper>
                ))}

                {pendingReviews.length === 0 && reviews.length === 0 && (
                  <Typography color="text.secondary">Nie masz jeszcze zajęć, które można ocenić.</Typography>
                )}
              </Stack>

              <Box>
                <Typography variant="h5">Lista oczekujących</Typography>
              </Box>
              <Stack spacing={1.5}>
                {waitlist.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2 }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.classTitle}</Typography>
                        <Typography color="text.secondary">
                          {new Date(item.startAt).toLocaleString('pl-PL')} · pozycja {item.position}
                        </Typography>
                      </Box>
                      {item.status === 'WAITING' && (
                        <Button color="error" onClick={() => leaveWaitlist(item.classId)}>
                          Opuść listę oczekujących
                        </Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {!bookingLoading && waitlist.length === 0 && (
                  <Typography color="text.secondary">Nie jesteś na liście oczekujących.</Typography>
                )}
              </Stack>

              <Box>
                <Typography variant="h5">Powiadomienia</Typography>
              </Box>
              <Stack spacing={1.5}>
                {notifications.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2, bgcolor: item.read ? 'transparent' : 'rgba(179, 136, 103, 0.08)' }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.title}</Typography>
                        <Typography color="text.secondary">{item.body}</Typography>
                        <Typography variant="body2" sx={{ mt: 0.5 }}>
                          {new Date(item.createdAt).toLocaleString('pl-PL')}
                        </Typography>
                      </Box>
                      {!item.read && (
                        <Button onClick={() => readNotification(item.id)}>Oznacz jako przeczytane</Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {notifications.length === 0 && <Typography color="text.secondary">Brak powiadomień.</Typography>}
              </Stack>
            </Stack>
          </>
        )}
      </Paper>
    </Container>
  )
}
