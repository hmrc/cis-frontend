cis-frontend
============

![](https://img.shields.io/github/v/release/hmrc/cis-frontend)

A Scala/Play frontend service for the [Construction Industry Scheme (CIS)](https://www.gov.uk/what-is-the-construction-industry-scheme) on the HMRC Tax Platform.

This service enables contractors and their agents to:

* File monthly CIS returns for subcontractors
* Submit nil (inactivity) returns
* Amend previously submitted returns
* Continue in-progress return journeys

The service is bilingual, supporting both English and Welsh.

## Running the service

Start all dependent services using Service Manager:

```shell
sm2 --start CIS_ALL
```

To start this service locally on port `6993`:

```shell
sbt run
```

To enable test-only routes when running locally:

```shell
sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes 6993'
```

### Upstream dependencies

| Service                          | Port |
|----------------------------------|------|
| `auth`                           | 8500 |
| `construction-industry-scheme`   | 6994 |
| `cis-manage-frontend`            | 6996 |
| `cis-contractor-frontend`        | 6998 |
| `feedback-frontend`              | 9514 |

## Git Hooks

This project includes a pre-push hook that checks code formatting with scalafmt before pushing.

To activate it, run once after cloning:

```shell
git config core.hooksPath hooks
```

If the check fails, format your code with `sbt scalafmtAll` and try again.

## Testing

Run unit and integration tests with coverage:

```shell
./run_all_tests.sh
```

Or run tests individually:

```shell
# Unit tests
sbt test

# Integration tests
sbt it/test

# Unit and integration tests with coverage report
sbt clean coverage test it/test coverageOff coverageReport
```

## Adding New Pages

### Folder Structure

The project uses domain-based organisation. Each new page should be placed in the appropriate domain folder:

```
app/
├── controllers/[domain]/          # e.g., monthlyreturns/
├── models/[domain]/               # e.g., monthlyreturns/
├── views/[domain]/                # e.g., monthlyreturns/
├── forms/[domain]/                # e.g., monthlyreturns/
├── pages/[domain]/                # e.g., monthlyreturns/
└── viewmodels/checkAnswers/[domain]/
```

```
test/
├── controllers/[domain]/
├── models/[domain]/
├── forms/[domain]/
└── views/[domain]/
```

### Example: routes and messages

```routes
GET  /monthly-return/submit-inactive-request  controllers.monthlyreturns.InactivityRequestController.onPageLoad(mode: Mode = NormalMode)
```

Message key (messages.en):

```properties
monthlyreturns.inactivityRequest.title = Do you want to submit an inactivity request?
```

## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
