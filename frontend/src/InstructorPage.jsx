import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
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
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded'
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded'
import QueueRoundedIcon from '@mui/icons-material/QueueRounded'
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded'
import { Navigate, useLocation } from 'react-router-dom'
import { fetchInstructorDashboard, fetchInstructorReviews, getApiError, replyToInstructorReview } from './api'
import { useAuth } from './auth-context'

const levelLabels = {
  BEGINNER: 'Początkujący',
  INTERMEDIATE: 'Średniozaawansowany',
  ADVANCED: 'Zaawansowany',
  ALL: 'Wszystkie poziomy',
}

const classStatusLabels = {
  DRAFT: 'Szkic',
  PUBLISHED: 'Opublikowane',
  CANCELLED: 'Anulowane',
  COMPLETED: 'Zakończone',
}

const replyRoleLabels = {
  INSTRUCTOR: 'Instruktor',
  ADMIN: 'Administrator',
}

const cardSx = {
  height: '100%',
  border: '1px solid rgba(49, 32, 61, 0.09)',
}

function formatDateTime(value) {
  return new Date(value).toLocaleString('pl-PL', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function isSameDay(value, now) {
  const date = new Date(value)
  return date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate()
}

function SummaryCard({ icon, label, value, helper }) {
  return (
    <Card variant="outlined" sx={cardSx}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={2}>
          <Box>
            <Typography color="text.secondary" variant="body2">{label}</Typography>
            <Typography variant="h4" sx={{ mt: 1 }}>{value}</Typography>
            <Typography color="text.secondary" variant="body2" sx={{ mt: 1 }}>
              {helper}
            </Typography>
          </Box>
          <Box color="primary.main">{icon}</Box>
        </Stack>
      </CardContent>
    </Card>
  )
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

function PeopleList({ title, items, emptyText, renderMeta }) {
  return (
    <Paper variant="outlined" sx={{ p: 2.5, height: '100%' }}>
      <Typography variant="h6">{title}</Typography>
      {items.length === 0 ? (
        <Typography color="text.secondary" sx={{ mt: 1.5 }}>{emptyText}</Typography>
      ) : (
        <Stack spacing={1.5} sx={{ mt: 2 }}>
          {items.map((item, index) => (
            <Box key={item.id || item.reservationId || `${item.email}-${index}`}>
              {index > 0 && <Divider sx={{ mb: 1.5 }} />}
              <Typography fontWeight={700}>{item.firstName} {item.lastName}</Typography>
              <Typography color="text.secondary" variant="body2">
                {item.email}{item.phone ? ` · ${item.phone}` : ''}
              </Typography>
              <Typography variant="body2" sx={{ mt: 0.5 }}>
                {renderMeta(item)}
              </Typography>
            </Box>
          ))}
        </Stack>
      )}
    </Paper>
  )
}

function InstructorClassCard({ item }) {
  return (
    <Card variant="outlined" sx={cardSx}>
      <CardContent sx={{ p: { xs: 2.5, md: 3 } }}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          justifyContent="space-between"
          alignItems={{ md: 'flex-start' }}
          gap={2}
        >
          <Box>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Chip size="small" color="secondary" label={item.danceStyle} />
              <Chip size="small" label={levelLabels[item.level] || item.level} />
              <Chip size="small" label={classStatusLabels[item.status] || item.status} />
            </Stack>
            <Typography variant="h5" sx={{ mt: 2 }}>{item.title}</Typography>
            {item.description && (
              <Typography color="text.secondary" sx={{ mt: 1 }}>{item.description}</Typography>
            )}
          </Box>
          <Box minWidth={{ md: 250 }}>
            <Stack spacing={1}>
              <Stack direction="row" spacing={1} alignItems="center">
                <AccessTimeRoundedIcon fontSize="small" />
                <Typography variant="body2">{formatDateTime(item.startAt)}</Typography>
              </Stack>
              <Stack direction="row" spacing={1} alignItems="center">
                <EventAvailableRoundedIcon fontSize="small" />
                <Typography variant="body2">{item.durationMinutes} min · {item.bookedPlaces}/{item.capacity} zapisanych</Typography>
              </Stack>
              <Stack direction="row" spacing={1} alignItems="center">
                <GroupsRoundedIcon fontSize="small" />
                <Typography variant="body2">{item.availablePlaces} wolnych miejsc</Typography>
              </Stack>
              <Stack direction="row" spacing={1} alignItems="center">
                <QueueRoundedIcon fontSize="small" />
                <Typography variant="body2">{item.waitlistCount} osób na liście oczekujących</Typography>
              </Stack>
            </Stack>
          </Box>
        </Stack>

        <Grid container spacing={2} sx={{ mt: 1 }}>
          <Grid item xs={12} md={7}>
            <PeopleList
              title="Zapisani klienci"
              items={item.participants}
              emptyText="Na te zajęcia nie ma jeszcze zapisanych klientów."
              renderMeta={(participant) => (
                <>
                  {participant.passName ? `Karnet: ${participant.passName}` : 'Bez przypisanego karnetu'}
                  {participant.bookedAt ? ` · zapis: ${new Date(participant.bookedAt).toLocaleString('pl-PL')}` : ''}
                </>
              )}
            />
          </Grid>
          <Grid item xs={12} md={5}>
            <PeopleList
              title="Lista oczekujących"
              items={item.waitingList}
              emptyText="Lista oczekujących jest pusta."
              renderMeta={(entry) => (
                <>
                  Pozycja {entry.position}
                  {entry.joinedAt ? ` · dołączono: ${new Date(entry.joinedAt).toLocaleString('pl-PL')}` : ''}
                </>
              )}
            />
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  )
}

export default function InstructorPage() {
  const { user, initializing } = useAuth()
  const location = useLocation()
  const [dashboard, setDashboard] = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [replyDrafts, setReplyDrafts] = useState({})
  const [replySubmittingId, setReplySubmittingId] = useState(null)

  useEffect(() => {
    if (!user || !['INSTRUCTOR', 'ADMIN'].includes(user.role)) {
      return
    }
    setLoading(true)
    setError('')
    setDashboard(null)
    setReviews([])
    Promise.all([
      fetchInstructorDashboard(),
      fetchInstructorReviews(),
    ])
      .then(([dashboardData, reviewData]) => {
        setDashboard(dashboardData)
        setReviews(reviewData)
      })
      .catch((requestError) => setError(getApiError(requestError, 'Nie udało się załadować panelu instruktora.')))
      .finally(() => setLoading(false))
  }, [user])

  const stats = useMemo(() => {
    const classes = dashboard?.classes || []
    const now = new Date()
    const upcoming = classes
      .filter((item) => item.status !== 'CANCELLED' && new Date(item.startAt).getTime() >= now.getTime())
      .sort((left, right) => new Date(left.startAt) - new Date(right.startAt))
    const today = upcoming.filter((item) => isSameDay(item.startAt, now))

    return {
      classes,
      upcoming,
      archive: classes
        .filter((item) => item.status === 'CANCELLED' || new Date(item.startAt).getTime() < now.getTime())
        .sort((left, right) => new Date(right.startAt) - new Date(left.startAt)),
      todayCount: today.length,
      upcomingCount: upcoming.length,
      participantCount: upcoming.reduce((sum, item) => sum + item.participants.length, 0),
      waitlistCount: upcoming.reduce((sum, item) => sum + item.waitingList.length, 0),
    }
  }, [dashboard])

  const reviewStats = useMemo(() => {
    if (!reviews.length) {
      return {
        count: 0,
        average: 0,
      }
    }
    const total = reviews.reduce((sum, item) => sum + item.rating, 0)
    return {
      count: reviews.length,
      average: Number((total / reviews.length).toFixed(1)),
    }
  }, [reviews])

  const changeReplyDraft = (reviewId, value) => {
    setReplyDrafts((current) => ({
      ...current,
      [reviewId]: value,
    }))
  }

  const submitReply = async (reviewId) => {
    const body = replyDrafts[reviewId]?.trim()
    if (!body) {
      setError('Wpisz treść odpowiedzi.')
      return
    }
    setError('')
    setReplySubmittingId(reviewId)
    try {
      const updated = await replyToInstructorReview(reviewId, body)
      setReviews((current) => current.map((item) => item.id === reviewId ? updated : item))
      setReplyDrafts((current) => {
        const next = { ...current }
        delete next[reviewId]
        return next
      })
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zapisać odpowiedzi'))
    } finally {
      setReplySubmittingId(null)
    }
  }

  if (initializing) {
    return <Box textAlign="center" py={12}><CircularProgress /></Box>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (!['INSTRUCTOR', 'ADMIN'].includes(user.role)) {
    return <Navigate to="/profile" replace />
  }

  if (loading) {
    return <Box textAlign="center" py={12}><CircularProgress /></Box>
  }

  return (
    <Container maxWidth="lg" sx={{ py: 8 }}>
      <Stack spacing={3}>
        <Box>
          <Typography variant="h2" fontSize={{ xs: 36, md: 50 }}>
            Panel instruktora
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>
            Twoje zajęcia, zapisani klienci i lista oczekujących w jednym miejscu.
          </Typography>
        </Box>

        {error && <Alert severity="error">{error}</Alert>}

        {!error && dashboard?.instructorId ? (
          <>
            <Paper variant="outlined" sx={{ p: { xs: 3, md: 4 } }}>
              <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2}>
                <Box>
                  <Typography variant="h4">
                    {dashboard.firstName} {dashboard.lastName}
                  </Typography>
                  <Typography color="text.secondary" sx={{ mt: 1 }}>
                    {dashboard.specialization || 'Instruktor'}
                  </Typography>
                  {dashboard.description && (
                    <Typography sx={{ mt: 2, maxWidth: 760 }}>
                      {dashboard.description}
                    </Typography>
                  )}
                </Box>
                <Box minWidth={{ md: 260 }}>
                  <Typography color="text.secondary" variant="body2">Wszystkie przypisane zajęcia</Typography>
                  <Typography variant="h3" sx={{ mt: 1 }}>{stats.classes.length}</Typography>
                </Box>
              </Stack>
            </Paper>

            <Grid container spacing={2}>
              <Grid item xs={12} md={3}>
                <SummaryCard
                  icon={<EventAvailableRoundedIcon fontSize="large" />}
                  label="Dzisiejsze zajęcia"
                  value={stats.todayCount}
                  helper="To, co masz do poprowadzenia dzisiaj."
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <SummaryCard
                  icon={<AccessTimeRoundedIcon fontSize="large" />}
                  label="Nadchodzące zajęcia"
                  value={stats.upcomingCount}
                  helper="Wszystkie przyszłe zajęcia poza anulowanymi."
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <SummaryCard
                  icon={<GroupsRoundedIcon fontSize="large" />}
                  label="Zapisani klienci"
                  value={stats.participantCount}
                  helper="Liczba klientów zapisanych na przyszłe zajęcia."
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <SummaryCard
                  icon={<QueueRoundedIcon fontSize="large" />}
                  label="Lista oczekujących"
                  value={stats.waitlistCount}
                  helper="Osoby czekające na wolne miejsce."
                />
              </Grid>
              <Grid item xs={12} md={3}>
                <SummaryCard
                  icon={<GroupsRoundedIcon fontSize="large" />}
                  label="Średnia ocen"
                  value={reviewStats.count ? reviewStats.average : '—'}
                  helper={reviewStats.count ? `${reviewStats.count} opinii od klientów.` : 'Brak opinii od klientów.'}
                />
              </Grid>
            </Grid>

            <Stack spacing={2}>
              <Box>
                <Typography variant="h4">Najbliższe zajęcia</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                  Tu od razu widzisz, ile osób przyjdzie i kto czeka na miejsce.
                </Typography>
              </Box>
              {stats.upcoming.length === 0 ? (
                <Paper variant="outlined" sx={{ p: 3 }}>
                  <Typography color="text.secondary">Nie masz jeszcze zaplanowanych zajęć.</Typography>
                </Paper>
              ) : (
                stats.upcoming.map((item) => <InstructorClassCard key={item.id} item={item} />)
              )}
            </Stack>

            {stats.archive.length > 0 && (
              <Stack spacing={2}>
                <Box>
                  <Typography variant="h4">Poprzednie i anulowane</Typography>
                  <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                    Ostatnie zajęcia w historii wraz ze stanem zapisów.
                  </Typography>
                </Box>
                {stats.archive.map((item) => <InstructorClassCard key={item.id} item={item} />)}
              </Stack>
            )}

            <Stack spacing={2}>
              <Box>
                <Typography variant="h4">Opinie klientów</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                  Tutaj możesz przeczytać oceny po zajęciach i odpowiedzieć klientom.
                </Typography>
              </Box>
              {reviews.length === 0 ? (
                <Paper variant="outlined" sx={{ p: 3 }}>
                  <Typography color="text.secondary">Nie ma jeszcze żadnych opinii.</Typography>
                </Paper>
              ) : (
                reviews.map((review) => (
                  <Paper variant="outlined" sx={{ p: 2.5 }} key={review.id}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
                      <Box>
                        <Typography fontWeight={700}>{review.clientName}</Typography>
                        <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                          {review.classTitle} · {formatDateTime(review.createdAt)}
                        </Typography>
                      </Box>
                      <Rating value={review.rating} readOnly />
                    </Stack>
                    {review.comment && <Typography sx={{ mt: 1.5 }}>{review.comment}</Typography>}
                    <ReviewReplies replies={review.replies} />

                    {user.role === 'INSTRUCTOR' && (
                      <Stack spacing={1.5} sx={{ mt: 2 }}>
                        <TextField
                          multiline
                          minRows={2}
                          label="Odpowiedź dla klienta"
                          value={replyDrafts[review.id] || ''}
                          onChange={(event) => changeReplyDraft(review.id, event.target.value)}
                        />
                        <Button
                          variant="contained"
                          sx={{ alignSelf: 'start' }}
                          disabled={replySubmittingId === review.id}
                          onClick={() => submitReply(review.id)}
                        >
                          Odpowiedz
                        </Button>
                      </Stack>
                    )}
                  </Paper>
                ))
              )}
            </Stack>
          </>
        ) : !error ? (
          <Paper variant="outlined" sx={{ p: { xs: 3, md: 4 } }}>
            <Typography variant="h5">
              To konto nie ma jeszcze przypisanego profilu instruktora.
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1.5, maxWidth: 760 }}>
              Gdy administrator przypisze Ci profil instruktora, w tym miejscu pojawią się
              Twoje zajęcia, liczba zapisanych klientów i lista oczekujących.
            </Typography>
          </Paper>
        ) : null}
      </Stack>
    </Container>
  )
}
