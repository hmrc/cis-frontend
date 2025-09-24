
# cis-frontend

This is the new cis-frontend repository

## Running the service

Service Manager: `sm2 --start CIS_ALL`

To run all tests and coverage: `./run_all_tests.sh`

To start the server locally: `sbt run`

To enable test-only routes when running locally, start the server with: `sbt 'run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes 6993'` 

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

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").