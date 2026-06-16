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
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import {
  cancelMyReservation,
  cancelMyPassOrder,
  createMyPassOrder,
  fetchMyNotifications,
  fetchMyPassOrders,
  fetchMyPasses,
  fetchPassTypes,
  fetchMyReservations,
  fetchMyWaitlist,
  getApiError,
  leaveMyWaitlist,
  markNotificationRead,
} from './api'
import { useAuth } from './auth-context'

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
      setError(getApiError(requestError, 'Не удалось загрузить записи и абонементы'))
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
      setError(getApiError(requestError, 'Не удалось загрузить покупки и уведомления'))
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
      setMessage('Профиль сохранён')
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось сохранить профиль'))
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
      setMessage('Запись отменена')
      await loadBookings()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось отменить запись'))
    }
  }

  const leaveWaitlist = async (classId) => {
    setError('')
    setMessage('')
    try {
      await leaveMyWaitlist(classId)
      setMessage('Вы вышли из листа ожидания')
      await loadBookings()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось выйти из листа ожидания'))
    }
  }

  const buyPass = async (passTypeId) => {
    setError('')
    setMessage('')
    try {
      await createMyPassOrder(passTypeId)
      setMessage('Заказ создан. Администратор подтвердит оплату после получения платежа.')
      await loadShop()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось создать заказ'))
    }
  }

  const cancelOrder = async (id) => {
    setError('')
    setMessage('')
    try {
      await cancelMyPassOrder(id)
      setMessage('Заказ отменён')
      await loadShop()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось отменить заказ'))
    }
  }

  const readNotification = async (id) => {
    try {
      const updated = await markNotificationRead(id)
      setNotifications((current) =>
        current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось обновить уведомление'))
    }
  }

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
            <Typography variant="overline" color="primary.main">{user.role}</Typography>
          </Box>
          <Button variant="outlined" onClick={signOut}>Выйти</Button>
        </Stack>

        <Box component="form" onSubmit={save} sx={{ mt: 5 }}>
          <Typography variant="h5" sx={{ mb: 2 }}>Личные данные</Typography>
          <Stack spacing={2}>
            {message && <Alert severity="success">{message}</Alert>}
            {error && <Alert severity="error">{error}</Alert>}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                fullWidth
                required
                label="Имя"
                value={form.firstName}
                onChange={(event) => setForm({ ...form, firstName: event.target.value })}
              />
              <TextField
                fullWidth
                required
                label="Фамилия"
                value={form.lastName}
                onChange={(event) => setForm({ ...form, lastName: event.target.value })}
              />
            </Stack>
            <TextField
              label="Телефон"
              value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })}
            />
            <Button type="submit" variant="contained" disabled={saving} sx={{ alignSelf: 'start' }}>
              {saving ? 'Сохранение...' : 'Сохранить'}
            </Button>
          </Stack>
        </Box>

        {user.role === 'CLIENT' && (
          <>
            <Divider sx={{ my: 5 }} />
            <Stack spacing={3}>
              <Box>
                <Typography variant="h5">Купить абонемент</Typography>
                <Typography color="text.secondary">Создайте заказ, затем администратор подтвердит оплату.</Typography>
              </Box>
              <Grid container spacing={2}>
                {passTypes.map((item) => (
                  <Grid item xs={12} md={4} key={item.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Typography fontWeight={700}>{item.name}</Typography>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>
                          {item.type === 'UNLIMITED' ? 'Безлимит' : `${item.visitCount} занятий`} · {item.validityDays} дней
                        </Typography>
                        <Typography variant="h6" sx={{ mt: 1 }}>{item.price} {item.currency}</Typography>
                        <Button fullWidth variant="contained" sx={{ mt: 2 }} onClick={() => buyPass(item.id)}>
                          Заказать
                        </Button>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>

              <Box>
                <Typography variant="h5">Мои заказы</Typography>
              </Box>
              <Stack spacing={1.5}>
                {orders.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2 }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.passName}</Typography>
                        <Typography color="text.secondary">
                          {item.amount} {item.currency} · {item.status}
                        </Typography>
                        <Typography variant="body2">
                          Создан {new Date(item.createdAt).toLocaleString('ru-RU')}
                        </Typography>
                      </Box>
                      {item.status === 'PENDING_PAYMENT' && (
                        <Button color="error" onClick={() => cancelOrder(item.id)}>
                          Отменить заказ
                        </Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {orders.length === 0 && <Typography color="text.secondary">Заказов пока нет.</Typography>}
              </Stack>

              <Box>
                <Typography variant="h5">Мои абонементы</Typography>
                <Typography color="text.secondary">Активные и использованные пакеты занятий</Typography>
              </Box>
              {bookingLoading && <CircularProgress size={24} />}
              <Grid container spacing={2}>
                {passes.map((item) => (
                  <Grid item xs={12} md={4} key={item.id}>
                    <Card variant="outlined">
                      <CardContent>
                        <Stack direction="row" justifyContent="space-between" gap={1}>
                          <Typography fontWeight={700}>{item.passName}</Typography>
                          <Chip size="small" label={item.status} />
                        </Stack>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>
                          {item.type === 'UNLIMITED' ? 'Безлимит' : `Осталось занятий: ${item.remainingVisits}`}
                        </Typography>
                        <Typography variant="body2" sx={{ mt: 1 }}>
                          До {new Date(item.validUntil).toLocaleDateString('ru-RU')}
                        </Typography>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
                {!bookingLoading && passes.length === 0 && (
                  <Grid item xs={12}><Typography color="text.secondary">Абонементов пока нет.</Typography></Grid>
                )}
              </Grid>

              <Box>
                <Typography variant="h5">Мои записи</Typography>
                <Typography color="text.secondary">Отмена позже чем за 12 часов считается поздней и занятие списывается.</Typography>
              </Box>
              <Stack spacing={1.5}>
                {reservations.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2 }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.classTitle}</Typography>
                        <Typography color="text.secondary">
                          {new Date(item.startAt).toLocaleString('ru-RU')} · {item.instructorName}
                        </Typography>
                        <Typography variant="body2">Абонемент: {item.passName || 'не указан'}</Typography>
                      </Box>
                      {item.status === 'CONFIRMED' && (
                        <Button color="error" onClick={() => cancelReservation(item.classId)}>
                          Отменить
                        </Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {!bookingLoading && reservations.length === 0 && (
                  <Typography color="text.secondary">Активных записей пока нет.</Typography>
                )}
              </Stack>

              <Box>
                <Typography variant="h5">Лист ожидания</Typography>
              </Box>
              <Stack spacing={1.5}>
                {waitlist.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2 }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.classTitle}</Typography>
                        <Typography color="text.secondary">
                          {new Date(item.startAt).toLocaleString('ru-RU')} · позиция {item.position}
                        </Typography>
                      </Box>
                      {item.status === 'WAITING' && (
                        <Button color="error" onClick={() => leaveWaitlist(item.classId)}>
                          Выйти из очереди
                        </Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {!bookingLoading && waitlist.length === 0 && (
                  <Typography color="text.secondary">В листе ожидания вас нет.</Typography>
                )}
              </Stack>

              <Box>
                <Typography variant="h5">Уведомления</Typography>
              </Box>
              <Stack spacing={1.5}>
                {notifications.map((item) => (
                  <Paper variant="outlined" sx={{ p: 2, bgcolor: item.read ? 'transparent' : 'rgba(179, 136, 103, 0.08)' }} key={item.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{item.title}</Typography>
                        <Typography color="text.secondary">{item.body}</Typography>
                        <Typography variant="body2" sx={{ mt: 0.5 }}>
                          {new Date(item.createdAt).toLocaleString('ru-RU')}
                        </Typography>
                      </Box>
                      {!item.read && (
                        <Button onClick={() => readNotification(item.id)}>Прочитано</Button>
                      )}
                    </Stack>
                  </Paper>
                ))}
                {notifications.length === 0 && <Typography color="text.secondary">Уведомлений пока нет.</Typography>}
              </Stack>
            </Stack>
          </>
        )}
      </Paper>
    </Container>
  )
}
