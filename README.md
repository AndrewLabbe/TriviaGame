## LEFT TODO

- Clients with same username but upper/lowercase
- Waiting to join replace question placeholder "this is where question 1 goes"

  Test Cases

- Client leaving/rejoining at each game-state ✅
- Client WIFI goes
- Client is the only one to poll then leaves/rejoins ✅
- Server disconnecting intermittently
- Client joins with same username as other connected client
- First client disconnects after polling, second client who buzzed must be considered the "first"

## REVIEW AFTER TODO

- Cases for server to use kill switch on Client

Doc says with no context:
(You would need to figure out a way so that server doesn’t get stuck in a deadlock or an infinite wait.)
so will note it maybe just do a sweep of code after
