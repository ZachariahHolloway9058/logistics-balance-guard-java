package example.logistics;

import java.time.Instant;

public record ShipmentEvent(String shipmentId, String status, Instant occurredAt) {}
