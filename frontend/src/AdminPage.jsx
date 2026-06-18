import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Rating,
  Stack,
  Switch,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import { Navigate } from 'react-router-dom'
import {
  cancelClassSession,
  cancelAdminPassOrder,
  changeUserRole,
  changeUserStatus,
  completePassOrderPayment,
  createClassSession,
  createInstructor,
  createPassType,
  fetchAdminClasses,
  fetchAdminInstructorDashboard,
  fetchAdminPassOrders,
  fetchAdminPassTypes,
  fetchAdminReviews,
  fetchAdminUsers,
  fetchInstructors,
  getApiError,
  grantUserPass,
  publishClassSession,
  replyToAdminReview,
  updateInstructor,
  updatePassType,
} from './api'
import { useAuth } from './auth-context'

const roleLabels = {
  CLIENT: 'Klient',
  INSTRUCTOR: 'Instruktor',
  ADMIN: 'Administrator',
}

const userStatusLabels = {
  PENDING: 'Oczekujący',
  ACTIVE: 'Aktywny',
  BLOCKED: 'Zablokowany',
  DEACTIVATED: 'Dezaktywowany',
}

const classStatusLabels = {
  DRAFT: 'Szkic',
  PUBLISHED: 'Opublikowane',
  IN_PROGRESS: 'W trakcie',
  CANCELLED: 'Anulowane',
  COMPLETED: 'Zakończone',
}

const passOrderStatusLabels = {
  PENDING_PAYMENT: 'Oczekuje na płatność',
  PAID: 'Opłacone',
  CANCELLED: 'Anulowane',
}

const levelLabels = {
  BEGINNER: 'Początkujący',
  INTERMEDIATE: 'Średniozaawansowany',
  ADVANCED: 'Zaawansowany',
  ALL: 'Wszystkie poziomy',
}

const replyRoleLabels = {
  INSTRUCTOR: 'Instruktor',
  ADMIN: 'Administrator',
}

const emptyInstructor = {
  userId: '',
  specialization: '',
  description: '',
}

const emptyClass = {
  title: '',
  description: '',
  danceStyle: '',
  level: 'BEGINNER',
  instructorId: '',
  capacity: 12,
  startAt: '',
  durationMinutes: 60,
}

const emptyPassType = {
  name: '',
  description: '',
  type: 'LIMITED',
  visitCount: 4,
  validityDays: 30,
  price: 0,
  active: true,
}

const emptyGrant = {
  userId: '',
  passTypeId: '',
  validFrom: '',
}

export default function AdminPage() {
  const { user, initializing } = useAuth()
  const [tab, setTab] = useState(0)
  const [users, setUsers] = useState([])
  const [instructors, setInstructors] = useState([])
  const [classes, setClasses] = useState([])
  const [passTypes, setPassTypes] = useState([])
  const [passOrders, setPassOrders] = useState([])
  const [reviews, setReviews] = useState([])
  const [selectedInstructorId, setSelectedInstructorId] = useState(null)
  const [selectedInstructorDashboard, setSelectedInstructorDashboard] = useState(null)
  const [instructorDashboardLoading, setInstructorDashboardLoading] = useState(false)
  const [instructorDashboardError, setInstructorDashboardError] = useState('')
  const [adminDataVersion, setAdminDataVersion] = useState(0)
  const [error, setError] = useState('')
  const [instructorDialog, setInstructorDialog] = useState(false)
  const [classDialog, setClassDialog] = useState(false)
  const [passDialog, setPassDialog] = useState(false)
  const [grantDialog, setGrantDialog] = useState(false)
  const [reviewDialog, setReviewDialog] = useState(false)
  const [reviewReplyBody, setReviewReplyBody] = useState('')
  const [selectedReview, setSelectedReview] = useState(null)
  const [replySubmitting, setReplySubmitting] = useState(false)
  const [instructorForm, setInstructorForm] = useState(emptyInstructor)
  const [classForm, setClassForm] = useState(emptyClass)
  const [passForm, setPassForm] = useState(emptyPassType)
  const [grantForm, setGrantForm] = useState(emptyGrant)

  const load = async () => {
    setError('')
    try {
      const [usersData, instructorsData, classesData, passTypeData, passOrderData, reviewData] = await Promise.all([
        fetchAdminUsers(),
        fetchInstructors(true),
        fetchAdminClasses(),
        fetchAdminPassTypes(),
        fetchAdminPassOrders(),
        fetchAdminReviews(),
      ])
      setUsers(usersData)
      setInstructors(instructorsData)
      setClasses(classesData)
      setPassTypes(passTypeData)
      setPassOrders(passOrderData)
      setReviews(reviewData)
      setAdminDataVersion((current) => current + 1)
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się załadować danych administracyjnych'))
    }
  }

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      load()
    }
  }, [user])

  useEffect(() => {
    if (!instructors.length) {
      setSelectedInstructorId(null)
      setSelectedInstructorDashboard(null)
      setInstructorDashboardError('')
      return
    }

    setSelectedInstructorId((current) =>
      instructors.some((item) => item.id === current) ? current : instructors[0].id)
  }, [instructors])

  useEffect(() => {
    if (user?.role !== 'ADMIN' || tab !== 1 || !selectedInstructorId) {
      return
    }

    let active = true
    setInstructorDashboardLoading(true)
    setInstructorDashboardError('')
    setSelectedInstructorDashboard(null)

    fetchAdminInstructorDashboard(selectedInstructorId)
      .then((data) => {
        if (active) {
          setSelectedInstructorDashboard(data)
        }
      })
      .catch((requestError) => {
        if (!active) {
          return
        }
        setInstructorDashboardError(getApiError(requestError, 'Nie udało się załadować grafiku instruktora'))
      })
      .finally(() => {
        if (active) {
          setInstructorDashboardLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [adminDataVersion, selectedInstructorId, tab, user])

  const instructorCandidates = useMemo(() => {
    const assigned = new Set(instructors.map((item) => item.userId))
    return users.filter((item) => !assigned.has(item.id) && item.status !== 'DEACTIVATED')
  }, [instructors, users])

  const clientUsers = useMemo(
    () => users.filter((item) => item.role === 'CLIENT' && item.status !== 'DEACTIVATED'),
    [users],
  )

  if (initializing) {
    return null
  }
  if (!user || user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  const mutateUser = async (id, type, value) => {
    try {
      const updated = type === 'role'
        ? await changeUserRole(id, value)
        : await changeUserStatus(id, value)
      setUsers((current) => current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zaktualizować użytkownika'))
    }
  }

  const submitInstructor = async () => {
    try {
      await createInstructor({
        ...instructorForm,
        userId: Number(instructorForm.userId),
      })
      setInstructorDialog(false)
      setInstructorForm(emptyInstructor)
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się utworzyć instruktora'))
    }
  }

  const toggleInstructor = async (instructor) => {
    try {
      const updated = await updateInstructor(instructor.id, {
        specialization: instructor.specialization,
        description: instructor.description,
        publicProfile: !instructor.publicProfile,
      })
      setInstructors((current) =>
        current.map((item) => item.id === instructor.id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zaktualizować instruktora'))
    }
  }

  const submitClass = async () => {
    try {
      await createClassSession({
        ...classForm,
        instructorId: Number(classForm.instructorId),
        capacity: Number(classForm.capacity),
        durationMinutes: Number(classForm.durationMinutes),
        startAt: new Date(classForm.startAt).toISOString(),
      })
      setClassDialog(false)
      setClassForm(emptyClass)
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się utworzyć zajęć'))
    }
  }

  const transitionClass = async (id, action) => {
    try {
      const updated = action === 'publish'
        ? await publishClassSession(id)
        : await cancelClassSession(id)
      setClasses((current) => current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zmienić statusu zajęć'))
    }
  }

  const passPayload = (form) => ({
    ...form,
    visitCount: form.type === 'UNLIMITED' ? null : Number(form.visitCount),
    validityDays: Number(form.validityDays),
    price: Number(form.price),
  })

  const submitPassType = async () => {
    try {
      await createPassType(passPayload(passForm))
      setPassDialog(false)
      setPassForm(emptyPassType)
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się utworzyć karnetu'))
    }
  }

  const togglePassType = async (passType) => {
    try {
      const updated = await updatePassType(passType.id, passPayload({
        ...passType,
        active: !passType.active,
      }))
      setPassTypes((current) => current.map((item) => item.id === passType.id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zaktualizować karnetu'))
    }
  }

  const submitGrant = async () => {
    try {
      await grantUserPass({
        userId: Number(grantForm.userId),
        passTypeId: Number(grantForm.passTypeId),
        validFrom: grantForm.validFrom ? new Date(grantForm.validFrom).toISOString() : null,
      })
      setGrantDialog(false)
      setGrantForm(emptyGrant)
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się przyznać karnetu'))
    }
  }

  const payOrder = async (id) => {
    try {
      const updated = await completePassOrderPayment(id, `manual-${id}`)
      setPassOrders((current) => current.map((item) => item.id === id ? updated : item))
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się potwierdzić płatności'))
    }
  }

  const cancelOrder = async (id) => {
    try {
      const updated = await cancelAdminPassOrder(id)
      setPassOrders((current) => current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się anulować zamówienia'))
    }
  }

  const openReplyDialog = (review) => {
    setSelectedReview(review)
    setReviewReplyBody('')
    setReviewDialog(true)
  }

  const submitReviewReply = async () => {
    if (!selectedReview) {
      return
    }
    if (!reviewReplyBody.trim()) {
      setError('Wpisz treść odpowiedzi.')
      return
    }
    setReplySubmitting(true)
    try {
      const updated = await replyToAdminReview(selectedReview.id, reviewReplyBody.trim())
      setReviews((current) => current.map((item) => item.id === updated.id ? updated : item))
      setReviewDialog(false)
      setSelectedReview(null)
      setReviewReplyBody('')
    } catch (requestError) {
      setError(getApiError(requestError, 'Nie udało się zapisać odpowiedzi'))
    } finally {
      setReplySubmitting(false)
    }
  }

  return (
    <Container maxWidth="xl" sx={{ py: 6 }}>
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2}>
        <Box>
          <Typography variant="h2" fontSize={{ xs: 38, md: 52 }}>Administracja</Typography>
          <Typography color="text.secondary">Użytkownicy, zespół i grafik</Typography>
        </Box>
        {tab === 1 && <Button variant="contained" onClick={() => setInstructorDialog(true)}>Dodaj instruktora</Button>}
        {tab === 2 && <Button variant="contained" onClick={() => setClassDialog(true)}>Utwórz zajęcia</Button>}
        {tab === 3 && (
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" onClick={() => setGrantDialog(true)}>Przyznaj karnet</Button>
            <Button variant="contained" onClick={() => setPassDialog(true)}>Utwórz karnet</Button>
          </Stack>
        )}
      </Stack>

      {error && <Alert severity="error" onClose={() => setError('')} sx={{ mt: 3 }}>{error}</Alert>}

      <Paper variant="outlined" sx={{ mt: 4 }}>
        <Tabs value={tab} onChange={(_, value) => setTab(value)} variant="scrollable">
          <Tab label="Użytkownicy" />
          <Tab label="Instruktorzy" />
          <Tab label="Grafik" />
          <Tab label="Karnety" />
          <Tab label="Opinie" />
        </Tabs>
        <Box sx={{ p: { xs: 1, md: 3 } }}>
          {tab === 0 && <UsersTable users={users} mutateUser={mutateUser} />}
          {tab === 1 && (
            <Stack spacing={3}>
              <InstructorsTable
                instructors={instructors}
                toggle={toggleInstructor}
                selectedInstructorId={selectedInstructorId}
                selectInstructor={setSelectedInstructorId}
              />
              <InstructorDashboardPanel
                dashboard={selectedInstructorDashboard}
                loading={instructorDashboardLoading}
                error={instructorDashboardError}
              />
            </Stack>
          )}
          {tab === 2 && <ClassesTable classes={classes} transition={transitionClass} />}
          {tab === 3 && (
            <Stack spacing={4}>
              <PassOrdersTable orders={passOrders} pay={payOrder} cancel={cancelOrder} />
              <PassTypesTable passTypes={passTypes} toggle={togglePassType} />
            </Stack>
          )}
          {tab === 4 && <ReviewsPanel reviews={reviews} openReplyDialog={openReplyDialog} />}
        </Box>
      </Paper>

      <InstructorDialog
        open={instructorDialog}
        users={instructorCandidates}
        form={instructorForm}
        setForm={setInstructorForm}
        close={() => setInstructorDialog(false)}
        submit={submitInstructor}
      />
      <ClassDialog
        open={classDialog}
        instructors={instructors}
        form={classForm}
        setForm={setClassForm}
        close={() => setClassDialog(false)}
        submit={submitClass}
      />
      <PassTypeDialog
        open={passDialog}
        form={passForm}
        setForm={setPassForm}
        close={() => setPassDialog(false)}
        submit={submitPassType}
      />
      <GrantPassDialog
        open={grantDialog}
        users={clientUsers}
        passTypes={passTypes.filter((item) => item.active)}
        form={grantForm}
        setForm={setGrantForm}
        close={() => setGrantDialog(false)}
        submit={submitGrant}
      />

      <Dialog open={reviewDialog} onClose={() => setReviewDialog(false)} fullWidth maxWidth="sm">
        <DialogTitle>Odpowiedź na opinię</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {selectedReview && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography fontWeight={700}>{selectedReview.clientName}</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                  {selectedReview.classTitle} · {new Date(selectedReview.createdAt).toLocaleString('pl-PL')}
                </Typography>
                <Rating value={selectedReview.rating} readOnly sx={{ mt: 1.5 }} />
                {selectedReview.comment && <Typography sx={{ mt: 1.5 }}>{selectedReview.comment}</Typography>}
              </Paper>
            )}
            <TextField
              multiline
              minRows={4}
              label="Treść odpowiedzi"
              value={reviewReplyBody}
              onChange={(event) => setReviewReplyBody(event.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReviewDialog(false)}>Anuluj</Button>
          <Button variant="contained" onClick={submitReviewReply} disabled={replySubmitting}>
            Odpowiedz
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  )
}

function ReviewRepliesList({ replies }) {
  if (!replies?.length) {
    return <Typography color="text.secondary" variant="body2" sx={{ mt: 1.5 }}>Brak odpowiedzi.</Typography>
  }

  return (
    <Stack spacing={1.25} sx={{ mt: 1.5 }}>
      {replies.map((reply) => (
        <Paper key={reply.id} variant="outlined" sx={{ p: 1.5, bgcolor: 'rgba(179, 136, 103, 0.05)' }}>
          <Typography fontWeight={700}>
            {replyRoleLabels[reply.authorRole] || reply.authorRole}: {reply.authorName}
          </Typography>
          <Typography variant="body2" sx={{ mt: 0.75 }}>{reply.body}</Typography>
          <Typography color="text.secondary" variant="caption" sx={{ mt: 0.75, display: 'block' }}>
            {new Date(reply.createdAt).toLocaleString('pl-PL')}
          </Typography>
        </Paper>
      ))}
    </Stack>
  )
}

function ReviewsPanel({ reviews, openReplyDialog }) {
  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5">Opinie klientów o instruktorach</Typography>
        <Typography color="text.secondary" sx={{ mt: 0.5 }}>
          Tutaj administrator może przeglądać oceny i odpowiadać na komentarze klientów.
        </Typography>
      </Box>

      {reviews.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography color="text.secondary">Nie ma jeszcze żadnych opinii.</Typography>
        </Paper>
      ) : (
        reviews.map((review) => (
          <Paper variant="outlined" sx={{ p: 2.5 }} key={review.id}>
            <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2}>
              <Box>
                <Typography fontWeight={700}>{review.clientName}</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                  {review.classTitle} · {review.instructorName}
                </Typography>
                <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
                  {new Date(review.createdAt).toLocaleString('pl-PL')}
                </Typography>
              </Box>
              <Box textAlign={{ md: 'right' }}>
                <Rating value={review.rating} readOnly />
                <Button sx={{ mt: { md: 1 } }} onClick={() => openReplyDialog(review)}>
                  Odpowiedz
                </Button>
              </Box>
            </Stack>
            {review.comment && <Typography sx={{ mt: 1.5 }}>{review.comment}</Typography>}
            <ReviewRepliesList replies={review.replies} />
          </Paper>
        ))
      )}
    </Stack>
  )
}

function UsersTable({ users, mutateUser }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Użytkownik</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>Rola</TableCell>
            <TableCell>Status</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {users.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{item.firstName} {item.lastName}</TableCell>
              <TableCell>{item.email}</TableCell>
              <TableCell>
                <TextField
                  select
                  size="small"
                  value={item.role}
                  onChange={(event) => mutateUser(item.id, 'role', event.target.value)}
                >
                  {['CLIENT', 'INSTRUCTOR', 'ADMIN'].map((role) =>
                    <MenuItem key={role} value={role}>{roleLabels[role] || role}</MenuItem>)}
                </TextField>
              </TableCell>
              <TableCell>
                <TextField
                  select
                  size="small"
                  value={item.status}
                  onChange={(event) => mutateUser(item.id, 'status', event.target.value)}
                >
                  {['PENDING', 'ACTIVE', 'BLOCKED', 'DEACTIVATED'].map((status) =>
                    <MenuItem key={status} value={status}>{userStatusLabels[status] || status}</MenuItem>)}
                </TextField>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function InstructorsTable({ instructors, toggle, selectedInstructorId, selectInstructor }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Instruktor</TableCell>
            <TableCell>Specjalizacja</TableCell>
            <TableCell>Opis</TableCell>
            <TableCell>Profil publiczny</TableCell>
            <TableCell>Panel</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {instructors.map((item) => (
            <TableRow key={item.id} hover selected={selectedInstructorId === item.id}>
              <TableCell>{item.firstName} {item.lastName}</TableCell>
              <TableCell>{item.specialization}</TableCell>
              <TableCell>{item.description || '—'}</TableCell>
              <TableCell>
                <Switch checked={item.publicProfile} onChange={() => toggle(item)} />
              </TableCell>
              <TableCell>
                <Button
                  size="small"
                  variant={selectedInstructorId === item.id ? 'contained' : 'outlined'}
                  onClick={() => selectInstructor(item.id)}
                >
                  {selectedInstructorId === item.id ? 'Wybrany' : 'Podgląd'}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function InstructorDashboardPanel({ dashboard, loading, error }) {
  if (loading) {
    return (
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography color="text.secondary">Ładowanie grafiku instruktora...</Typography>
      </Paper>
    )
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>
  }

  if (!dashboard) {
    return (
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography color="text.secondary">Wybierz instruktora, aby zobaczyć jego grafik i klientów.</Typography>
      </Paper>
    )
  }

  const activeClasses = dashboard.classes.filter((item) => item.status !== 'CANCELLED')
  const participantCount = activeClasses.reduce((sum, item) => sum + item.participants.length, 0)
  const waitlistCount = activeClasses.reduce((sum, item) => sum + item.waitingList.length, 0)

  return (
    <Stack spacing={2}>
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="h5">
          {dashboard.firstName} {dashboard.lastName}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.75 }}>
          {dashboard.specialization || 'Bez wskazanej specjalizacji'}
        </Typography>
        {dashboard.description && (
          <Typography sx={{ mt: 1.5 }}>{dashboard.description}</Typography>
        )}
        <Typography color="text.secondary" sx={{ mt: 1.5 }}>
          Zajęcia: {dashboard.classes.length} · zapisani klienci: {participantCount} · lista oczekujących: {waitlistCount}
        </Typography>
      </Paper>

      {dashboard.classes.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 3 }}>
          <Typography color="text.secondary">Ten instruktor nie ma jeszcze przypisanych zajęć.</Typography>
        </Paper>
      ) : (
        dashboard.classes.map((item) => (
          <Paper key={item.id} variant="outlined" sx={{ p: 2.5 }}>
            <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2}>
              <Box>
                <Typography variant="h6">{item.title}</Typography>
                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                  {new Date(item.startAt).toLocaleString('pl-PL')} · {item.danceStyle} · {levelLabels[item.level] || item.level}
                </Typography>
                {item.description && (
                  <Typography sx={{ mt: 1 }}>{item.description}</Typography>
                )}
              </Box>
              <Box>
                <Typography fontWeight={700}>{classStatusLabels[item.status] || item.status}</Typography>
                <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
                  Zapisani: {item.bookedPlaces}/{item.capacity}
                </Typography>
                <Typography color="text.secondary" variant="body2">
                  Wolne miejsca: {item.availablePlaces}
                </Typography>
                <Typography color="text.secondary" variant="body2">
                  Lista oczekujących: {item.waitlistCount}
                </Typography>
              </Box>
            </Stack>

            <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} sx={{ mt: 2 }}>
              <Paper variant="outlined" sx={{ p: 2, flex: 1 }}>
                <Typography fontWeight={700}>Zapisani klienci</Typography>
                {item.participants.length === 0 ? (
                  <Typography color="text.secondary" sx={{ mt: 1.5 }}>
                    Brak zapisanych klientów.
                  </Typography>
                ) : (
                  <Stack spacing={1.25} sx={{ mt: 1.5 }}>
                    {item.participants.map((participant) => (
                      <Box key={participant.reservationId}>
                        <Typography fontWeight={600}>{participant.firstName} {participant.lastName}</Typography>
                        <Typography color="text.secondary" variant="body2">
                          {participant.email}{participant.phone ? ` · ${participant.phone}` : ''}
                        </Typography>
                        <Typography variant="body2" sx={{ mt: 0.5 }}>
                          {participant.passName ? `Karnet: ${participant.passName}` : 'Bez przypisanego karnetu'}
                          {participant.bookedAt
                            ? ` · zapis: ${new Date(participant.bookedAt).toLocaleString('pl-PL')}`
                            : ''}
                        </Typography>
                      </Box>
                    ))}
                  </Stack>
                )}
              </Paper>

              <Paper variant="outlined" sx={{ p: 2, flex: 1 }}>
                <Typography fontWeight={700}>Lista oczekujących</Typography>
                {item.waitingList.length === 0 ? (
                  <Typography color="text.secondary" sx={{ mt: 1.5 }}>
                    Lista oczekujących jest pusta.
                  </Typography>
                ) : (
                  <Stack spacing={1.25} sx={{ mt: 1.5 }}>
                    {item.waitingList.map((entry) => (
                      <Box key={entry.id}>
                        <Typography fontWeight={600}>{entry.firstName} {entry.lastName}</Typography>
                        <Typography color="text.secondary" variant="body2">
                          {entry.email}{entry.phone ? ` · ${entry.phone}` : ''}
                        </Typography>
                        <Typography variant="body2" sx={{ mt: 0.5 }}>
                          Pozycja {entry.position}
                          {entry.joinedAt
                            ? ` · dołączono: ${new Date(entry.joinedAt).toLocaleString('pl-PL')}`
                            : ''}
                        </Typography>
                      </Box>
                    ))}
                  </Stack>
                )}
              </Paper>
            </Stack>
          </Paper>
        ))
      )}
    </Stack>
  )
}

function ClassesTable({ classes, transition }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Zajęcia</TableCell>
            <TableCell>Instruktor</TableCell>
            <TableCell>Początek</TableCell>
            <TableCell>Miejsca</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Akcje</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {classes.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{item.title}<br /><small>{item.danceStyle}</small></TableCell>
              <TableCell>{item.instructorName}</TableCell>
              <TableCell>{new Date(item.startAt).toLocaleString('pl-PL')}</TableCell>
              <TableCell>{item.availablePlaces}/{item.capacity}</TableCell>
              <TableCell>{classStatusLabels[item.status] || item.status}</TableCell>
              <TableCell>
                <Stack direction="row" spacing={1}>
                  {item.status === 'DRAFT' && (
                    <Button size="small" onClick={() => transition(item.id, 'publish')}>Opublikuj</Button>
                  )}
                  {!['CANCELLED', 'COMPLETED', 'IN_PROGRESS'].includes(item.status) && (
                    <Button size="small" color="error" onClick={() => transition(item.id, 'cancel')}>Anuluj</Button>
                  )}
                </Stack>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function PassTypesTable({ passTypes, toggle }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Karnet</TableCell>
            <TableCell>Typ</TableCell>
            <TableCell>Okres</TableCell>
            <TableCell>Cena</TableCell>
            <TableCell>Aktywny</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {passTypes.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{item.name}<br /><small>{item.description || 'bez opisu'}</small></TableCell>
              <TableCell>{item.type === 'UNLIMITED' ? 'Bez limitu' : `${item.visitCount} wejść`}</TableCell>
              <TableCell>{item.validityDays} dni</TableCell>
              <TableCell>{item.price} {item.currency}</TableCell>
              <TableCell>
                <Switch checked={item.active} onChange={() => toggle(item)} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function PassOrdersTable({ orders, pay, cancel }) {
  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Zamówienia karnetów</Typography>
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Klient</TableCell>
              <TableCell>Karnet</TableCell>
              <TableCell>Kwota</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Utworzono</TableCell>
              <TableCell>Akcje</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.map((item) => (
              <TableRow key={item.id}>
                <TableCell>{item.userName}</TableCell>
                <TableCell>{item.passName}</TableCell>
                <TableCell>{item.amount} {item.currency}</TableCell>
                <TableCell>{passOrderStatusLabels[item.status] || item.status}</TableCell>
                <TableCell>{new Date(item.createdAt).toLocaleString('pl-PL')}</TableCell>
                <TableCell>
                  {item.status === 'PENDING_PAYMENT' && (
                    <Stack direction="row" spacing={1}>
                      <Button size="small" onClick={() => pay(item.id)}>Potwierdź płatność</Button>
                      <Button size="small" color="error" onClick={() => cancel(item.id)}>Anuluj</Button>
                    </Stack>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}

function InstructorDialog({ open, users, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Nowy instruktor</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            select
            label="Użytkownik"
            value={form.userId}
            onChange={(event) => setForm({ ...form, userId: event.target.value })}
          >
            {users.map((item) => (
              <MenuItem key={item.id} value={item.id}>
                {item.firstName} {item.lastName} — {item.email}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Specjalizacja"
            value={form.specialization}
            onChange={(event) => setForm({ ...form, specialization: event.target.value })}
          />
          <TextField
            label="Opis"
            multiline
            minRows={3}
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Anuluj</Button>
        <Button variant="contained" onClick={submit} disabled={!form.userId || !form.specialization}>
          Utwórz
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function ClassDialog({ open, instructors, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Nowe zajęcia</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Nazwa" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          <TextField label="Opis" multiline minRows={2} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
          <TextField label="Styl" value={form.danceStyle} onChange={(event) => setForm({ ...form, danceStyle: event.target.value })} />
          <TextField select label="Poziom" value={form.level} onChange={(event) => setForm({ ...form, level: event.target.value })}>
            {['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ALL'].map((level) => <MenuItem key={level} value={level}>{levelLabels[level] || level}</MenuItem>)}
          </TextField>
          <TextField select label="Instruktor" value={form.instructorId} onChange={(event) => setForm({ ...form, instructorId: event.target.value })}>
            {instructors.map((item) => <MenuItem key={item.id} value={item.id}>{item.firstName} {item.lastName}</MenuItem>)}
          </TextField>
          <TextField type="datetime-local" label="Data i godzina" InputLabelProps={{ shrink: true }} value={form.startAt} onChange={(event) => setForm({ ...form, startAt: event.target.value })} />
          <Stack direction="row" spacing={2}>
            <TextField fullWidth type="number" label="Pojemność" value={form.capacity} onChange={(event) => setForm({ ...form, capacity: event.target.value })} />
            <TextField fullWidth type="number" label="Czas trwania, min" value={form.durationMinutes} onChange={(event) => setForm({ ...form, durationMinutes: event.target.value })} />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Anuluj</Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={!form.title || !form.danceStyle || !form.instructorId || !form.startAt}
        >
          Utwórz szkic
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function PassTypeDialog({ open, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Nowy karnet</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Nazwa" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <TextField label="Opis" multiline minRows={2} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
          <TextField select label="Typ" value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
            <MenuItem value="LIMITED">Limit wejść</MenuItem>
            <MenuItem value="UNLIMITED">Bez limitu</MenuItem>
          </TextField>
          {form.type === 'LIMITED' && (
            <TextField type="number" label="Liczba wejść" value={form.visitCount} onChange={(event) => setForm({ ...form, visitCount: event.target.value })} />
          )}
          <Stack direction="row" spacing={2}>
            <TextField fullWidth type="number" label="Okres ważności, dni" value={form.validityDays} onChange={(event) => setForm({ ...form, validityDays: event.target.value })} />
            <TextField fullWidth type="number" label="Cena" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Anuluj</Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={!form.name || !form.validityDays || (form.type === 'LIMITED' && !form.visitCount)}
        >
          Utwórz
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function GrantPassDialog({ open, users, passTypes, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Przyznaj karnet</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField select label="Klient" value={form.userId} onChange={(event) => setForm({ ...form, userId: event.target.value })}>
            {users.map((item) => (
              <MenuItem key={item.id} value={item.id}>
                {item.firstName} {item.lastName} — {item.email}
              </MenuItem>
            ))}
          </TextField>
          <TextField select label="Karnet" value={form.passTypeId} onChange={(event) => setForm({ ...form, passTypeId: event.target.value })}>
            {passTypes.map((item) => (
              <MenuItem key={item.id} value={item.id}>
                {item.name} · {item.type === 'UNLIMITED' ? 'bez limitu' : `${item.visitCount} wejść`}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            type="datetime-local"
            label="Ważny od"
            InputLabelProps={{ shrink: true }}
            value={form.validFrom}
            onChange={(event) => setForm({ ...form, validFrom: event.target.value })}
            helperText="Jeśli pozostawisz puste, zacznie obowiązywać od teraz"
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Anuluj</Button>
        <Button variant="contained" onClick={submit} disabled={!form.userId || !form.passTypeId}>
          Przyznaj
        </Button>
      </DialogActions>
    </Dialog>
  )
}
