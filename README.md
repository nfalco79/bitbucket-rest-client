# Bitbucket REST client

This project implements a reusable REST client that use bitbucket cloud APIs.
I decided to not implement a bitbucket server version because since 2 feb 2021 no more license will be sell.

## Implementation
Most of REST 2.0 APIs are implemented, some one use REST 1.0 APIs because not available in 2.0.
Some methods rely on internal APIs but will not work in a short term because atlassian will remove the Altassian autentication to calls APIs.
The internal APIs does not accept authentication based on access token (OAuth2 Consumer/App password).
