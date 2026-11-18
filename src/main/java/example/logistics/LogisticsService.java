package example.logistics;

import java.time.Instant;

public final class LogisticsService {
    private final BalanceGuard balanceGuard;

    public LogisticsService(BalanceGuard balanceGuard) { this.balanceGuard = balanceGuard; }

    public ShipmentEvent accept(ProofOfDelivery proof, double balance) throws Exception {
        balanceGuard.prepare(balance, 5.0);
        return new ShipmentEvent(proof.shipmentId(), "proof-recorded", Instant.now());
    }
}
