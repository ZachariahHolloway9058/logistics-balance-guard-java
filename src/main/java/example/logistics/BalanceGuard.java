package example.logistics;

import java.io.IOException;

public final class BalanceGuard {
    private final InfraiClient infrai;
    private final double rechargeAmount;
    private final String notificationRecipient;

    public BalanceGuard(InfraiClient infrai, double rechargeAmount, String notificationRecipient) {
        this.infrai = infrai;
        this.rechargeAmount = rechargeAmount;
        this.notificationRecipient = notificationRecipient;
    }

    public Decision prepare(double currentBalance, double triggerBalance) throws IOException, InterruptedException {
        if (currentBalance > triggerBalance) return new Decision(false, "dispatch");
        infrai.configureAutorecharge(triggerBalance, rechargeAmount);
        infrai.sendEmail(notificationRecipient, "Shipment balance protected", "Automatic recharge configured before dispatch.");
        return new Decision(true, "recharge-configured-and-notified");
    }

    public record Decision(boolean rechargeConfigured, String action) {}
}
