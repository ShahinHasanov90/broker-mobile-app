# broker-mobile-app

Android companion application for licensed customs brokers. The app is the
field-side counterpart to a back-office declaration management system: it lets
the principal broker review draft declarations prepared by operators, approve
or reject them before they are lodged with customs, and track the status of
submitted declarations while moving between warehouses, terminals and border
crossings where connectivity is unreliable.

## Domain

The broker is the licensed professional who is ultimately responsible for the
accuracy of a customs declaration. Operators prepare the line items and attach
supporting documents (invoices, packing lists, certificates of origin), but a
declaration cannot be lodged until the broker principal has given explicit
approval. Once lodged, customs returns a sequence of status updates (Accepted,
Rejected, Released) that the broker must be able to see without opening a
laptop.

## Screens

- Declarations list: chronological feed of declarations assigned to the
  broker, each row shows declarant, reference, HS chapter summary and a
  colour-coded status chip (Draft, Submitted, Accepted, Rejected, Released).
- Declaration detail: header fields, line items with HS code and customs
  value, attachments, and a status history timeline.
- Approval: review pane for draft declarations with Approve and Reject
  actions. Rejection requires a reason that is sent back to the operator.
- Notifications: system tray notifications fire when customs changes the
  status of a declaration that the broker is watching.

## Architecture

The app follows an MVVM structure on top of Jetpack libraries:

- UI layer is built entirely with Jetpack Compose. Each screen has a
  stateless composable and a `ViewModel` that owns a `StateFlow` of
  immutable UI state.
- Navigation uses the Compose Navigation library. The top-level graph is
  defined in `BrokerNavGraph.kt`.
- Dependency injection is handled by Hilt. Application-scoped singletons
  (database, Retrofit, OkHttp client) are declared in the `di` package.
- The data layer exposes a single `DeclarationRepository` to the UI. The
  repository coordinates three sources: the Room cache, the Retrofit-backed
  `BrokerApi` and an outbox table for writes that cannot be delivered yet.
- Persistence uses Room. `DeclarationEntity` mirrors the server model closely
  enough to support offline browsing; `OutboxEntity` stores pending mutations
  with an idempotency key.

## Offline strategy

Read path: the list and detail screens always read from Room through Flow.
When the app starts and whenever the repository is refreshed, remote data is
fetched and written to the cache, so the UI update is driven by the database
observer. Losing connectivity does not change the behaviour of the read path.

Write path: approval and rejection actions write an `OutboxEntity` in the
same transaction that updates the local declaration. An `OutboxWorker`
scheduled with WorkManager drains the outbox whenever the device has
connectivity. Each outbox row carries an idempotency key so that retries do
not produce duplicate server-side state. If the server responds with a
conflict the worker marks the row as failed and the user is notified on next
launch.

## Modules and build

- Kotlin 2.x with the Compose compiler plugin.
- Single `:app` module; the project is intentionally small so the package
  layout (`ui/`, `data/`, `di/`) does the boundary work that a multi-module
  setup would otherwise do.
- Unit tests use JUnit 4, Turbine for Flow assertions and the official
  coroutines test dispatcher. The Retrofit client is not exercised in tests;
  the repository accepts a `BrokerApi` interface and tests pass a fake.

## Running locally

Open the project in Android Studio Koala or later, let Gradle sync, and run
the `app` configuration on an API 26+ emulator. The Retrofit base URL in
`NetworkModule` points at a placeholder host; flip it to a local mock server
to exercise the full network path.
