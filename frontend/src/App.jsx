import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  AppBar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Toolbar,
  Typography,
} from '@mui/material'
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded'
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded'
import { Link as RouterLink, Route, Routes, useNavigate } from 'react-router-dom'
import { bookClass, fetchClasses, getApiError } from './api'
import { useAuth } from './auth-context'
import { LoginPage, RegisterPage } from './AuthPage'
import ProfilePage from './ProfilePage'
import AdminPage from './AdminPage'

const levelLabels = {
  BEGINNER: 'Начинающий',
  INTERMEDIATE: 'Средний',
  ADVANCED: 'Продвинутый',
  ALL: 'Все уровни',
}

function Header() {
  const { user } = useAuth()

  return (
    <AppBar position="static" elevation={0} color="transparent">
      <Toolbar>
        <Typography
          component={RouterLink}
          to="/"
          variant="h6"
          color="inherit"
          sx={{ flexGrow: 1, fontFamily: 'Georgia, serif', textDecoration: 'none' }}
        >
          DIANA
        </Typography>
        {user ? (
          <Stack direction="row" spacing={1}>
            {user.role === 'ADMIN' && (
              <Button component={RouterLink} to="/admin" color="inherit">Админ-панель</Button>
            )}
            <Button component={RouterLink} to="/profile" variant="contained">
              {user.firstName}
            </Button>
          </Stack>
        ) : (
          <>
            <Button component={RouterLink} to="/login" color="inherit">Войти</Button>
            <Button component={RouterLink} to="/register" variant="contained">
              Регистрация
            </Button>
          </>
        )}
      </Toolbar>
    </AppBar>
  )
}

function ClassCard({ item, onBook, busy }) {
  const date = new Intl.DateTimeFormat('ru-RU', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(item.startAt))

  return (
    <Card className="class-card" variant="outlined">
      <CardContent>
        <Stack direction="row" justifyContent="space-between" gap={2}>
          <Chip label={item.danceStyle} color="secondary" size="small" />
          <Typography color="text.secondary" variant="body2">
            {levelLabels[item.level] || item.level}
          </Typography>
        </Stack>
        <Typography variant="h5" sx={{ mt: 2 }}>{item.title}</Typography>
        <Typography color="text.secondary" sx={{ mt: 1, minHeight: 48 }}>
          {item.description}
        </Typography>
        <Typography fontWeight={600} sx={{ mt: 2 }}>{item.instructorName}</Typography>
        <Stack spacing={1} sx={{ mt: 2 }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <AccessTimeRoundedIcon fontSize="small" />
            <Typography variant="body2">{date}, {item.durationMinutes} мин.</Typography>
          </Stack>
          <Stack direction="row" spacing={1} alignItems="center">
            <GroupsRoundedIcon fontSize="small" />
            <Typography variant="body2">
              Свободно {item.availablePlaces} из {item.capacity}
            </Typography>
          </Stack>
        </Stack>
        <Button fullWidth variant="contained" sx={{ mt: 3 }} onClick={() => onBook(item)} disabled={busy}>
          {item.availablePlaces > 0 ? 'Записаться' : 'Встать в лист ожидания'}
        </Button>
      </CardContent>
    </Card>
  )
}

function HomePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [classes, setClasses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [bookingId, setBookingId] = useState(null)
  const [style, setStyle] = useState('')

  const loadClasses = () => {
    setLoading(true)
    return fetchClasses()
      .then(setClasses)
      .catch(() => setError('Не удалось загрузить расписание. Проверьте backend.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadClasses()
  }, [])

  const handleBook = async (item) => {
    setError('')
    setMessage('')
    if (!user) {
      navigate('/login', { state: { from: '/' } })
      return
    }
    if (user.role !== 'CLIENT') {
      setError('Запись доступна только клиентам школы.')
      return
    }
    setBookingId(item.id)
    try {
      const result = await bookClass(item.id)
      setMessage(result.result === 'CONFIRMED'
        ? 'Вы записаны на занятие. Абонемент зарезервирован.'
        : `Свободных мест нет, вы в листе ожидания под номером ${result.queuePosition}.`)
      await loadClasses()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось записаться на занятие'))
    } finally {
      setBookingId(null)
    }
  }

  const styles = useMemo(
    () => [...new Set(classes.map((item) => item.danceStyle))],
    [classes],
  )
  const visibleClasses = style
    ? classes.filter((item) => item.danceStyle === style)
    : classes

  return (
    <>
      <Box className="hero">
        <Container maxWidth="lg">
          <Typography variant="overline" color="secondary.main">
            Школа танцев в Варшаве
          </Typography>
          <Typography variant="h1" className="hero-title">Найди свой ритм</Typography>
          <Typography className="hero-copy">
            Выбирай направление, знакомься с преподавателями и бронируй занятие онлайн.
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: 7 }}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ sm: 'center' }}
          gap={2}
          sx={{ mb: 4 }}
        >
          <Box>
            <Typography variant="h2" fontSize={{ xs: 36, md: 48 }}>
              Ближайшие занятия
            </Typography>
            <Typography color="text.secondary">Живое расписание школы</Typography>
          </Box>
          <TextField
            select
            label="Направление"
            value={style}
            onChange={(event) => setStyle(event.target.value)}
            sx={{ minWidth: 220 }}
          >
            <MenuItem value="">Все направления</MenuItem>
            {styles.map((item) => <MenuItem key={item} value={item}>{item}</MenuItem>)}
          </TextField>
        </Stack>

        {loading && <Box textAlign="center"><CircularProgress /></Box>}
        {message && <Alert severity="success" sx={{ mb: 3 }} onClose={() => setMessage('')}>{message}</Alert>}
        {error && <Alert severity="warning" sx={{ mb: 3 }} onClose={() => setError('')}>{error}</Alert>}
        {!loading && classes.length > 0 && (
          <Grid container spacing={3}>
            {visibleClasses.map((item) => (
              <Grid item key={item.id} xs={12} md={4}>
                <ClassCard item={item} onBook={handleBook} busy={bookingId === item.id} />
              </Grid>
            ))}
          </Grid>
        )}
      </Container>
    </>
  )
}

export default function App() {
  return (
    <>
      <Header />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Routes>
    </>
  )
}
