# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**

## Design Notes

Went with a simple approach - validation happens in the service before
calling the bank. If anything's wrong with the request, we reject it
straight away without making unnecessary bank calls.

Bank communication is in its own class (BankClient) using RestTemplate.
Returns Optional.empty() when the bank is down (503) rather than letting
exceptions bubble up.

Card number comes in full from the merchant but we only store and return
the last 4 digits. CVV is a String not int to preserve leading zeros.

Supported currencies: GBP, USD, EUR.

Rejected payments are not persisted since they never went through the bank.

### Things I'd add with more time
- Idempotency keys to prevent duplicate payments
- Retry logic for transient bank failures
- Request logging with correlation IDs
- More granular error responses (which fields failed validation)