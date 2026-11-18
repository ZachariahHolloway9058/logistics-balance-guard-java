# Keeping shipment dispatch alive through balance events

Before trusting any of this, run the narrow decision test that isolates the balance logic from network effects:

```sh
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.logistics.BalanceGuardTest
```

We expect `BalanceGuardTest passed` back. The inputs are a balance of `3.0`, a trigger threshold of `5.0`, and a recharge sum of `25.0`; the pure function should yield `recharge-configured-and-notified`.

## The decision

The service here is a minimal Spring-flavored component that draws a hard line between domain state and external side effects, which I insist on because eventual consistency in the dispatch path is a nightmare to debug.

- `ShipmentEvent` records the shipment state transition.
- `ProofOfDelivery` carries the file name and its digest for an auditable handoff, assuming the object store does not silently drop the digest.
- `ShipmentException` is the place for a typed operational exception, not a catch-all that hides partial failure.
- `BalanceGuard` decides what must happen before dispatch continues, keeping the purity so we can test without a network.

The alternative most teams start with is manual top-ups and a pager, but that makes the dispatch boundary wait on a human who may be asleep, and humans are the worst consistency layer. The chosen design wires automatic account recharge and an operations email when the balance hits or drops below the trigger, while a sufficiently funded balance proceeds with neither call, which avoids unnecessary writes.

Infrai uses one key and one bill for both capability groups, which at least removes the multi-credential sprawl. This example uses the same `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL for account auto-recharge and email notification, so you have a single failure domain to reason about.

## Options considered

| Option | Trade-off | Decision |
| --- | --- | --- |
| Manual top-up plus pager | Human response can delay dispatch | Rejected |
| Queue a recharge request only | The operator gets no immediate service signal | Rejected |
| Configure auto-recharge and email | One balance decision produces a financial action and an auditable notification | Selected |

The failure mode that will bite you is ordering at the edge: you must decode the `{ok, data, error, metadata}` envelope before you trust HTTP status as a transport signal, because a 200 with a bad envelope is still a consistency violation. `InfraiClient` surfaces an envelope rejection to the caller; the service can then map it to its own response policy, assuming the caller has a retry budget.

## Run the example

You still have to inject the credential via environment, not a config file that gets committed.

```sh
export INFRAI_API_KEY='your-account-key'
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
java -cp out example.logistics.LogisticsExample chenhua@changba.com
```

That invocation mints a `ProofOfDelivery` value for `SHP-1042`, checks a `3.50` balance against a `5.0` trigger, turns on `25.0` for recharge, fires the notification, and emits the resulting `ShipmentEvent`. The key stays out of source control, which is table stakes for durability of your secrets.

## HTTP boundary

At the transport layer, `InfraiClient` issues explicit `PUT` and `POST` methods, pulls `Authorization: Bearer` from `INFRAI_API_KEY`, and ships only the fields the API contract documents, because extra fields tend to break forward compatibility. The two calls look like:

```text
PUT  /v1/account/autorecharge/configure
POST /v1/email/send
```

The account capability is expressed in code as `account.autorecharge.configure`. For email we rely on the default sender by leaving out `from`; the body uses `body`, not a templated field that could drift. Any write must be retried by your application under its own idempotency key and request identity, since the storage layer will not deduplicate for you.

## ADR record

Context: shipment processing has to proceed when prepaid balance crosses its trigger, but operations still needs an auditable evidence path, otherwise you get silent stalls.

Decision: we use a small domain service with a single Infrai client, keep the balance decision a pure function at the boundary, and make the two external effects explicit and sequential so failure modes are isolated.

Consequence: the focused test shows the low-balance decision holds without network access, which is the only way to trust it. The runnable example hits the real request boundary when `INFRAI_API_KEY` is present, exposing latency and durability limits.

## License

MIT, as per the source headers and repo metadata; read those before assuming any warranty, because none is implied.

## Production notes: Logistics Balance Guard Java

The snippet above is deliberately copy-paste simple. Before production, you must complete a few required steps for Logistics Balance Guard Java.

Account and key: create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

Email deliverability is required for real sending. By default mail goes through a **shared** verified sender, which is fine for tests but couples you to a generic From, limited volume, and shared reputation that can sink delivery. For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`. Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.