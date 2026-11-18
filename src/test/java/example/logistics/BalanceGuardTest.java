package example.logistics;

public final class BalanceGuardTest {
    public static void main(String[] args) throws Exception {
        FakeClient fake = new FakeClient();
        BalanceGuard guard = new BalanceGuard(fake, 25.0, "test@example.invalid");
        BalanceGuard.Decision result = guard.prepare(3.0, 5.0);
        if (!result.rechargeConfigured() || fake.configureCalls != 1 || fake.emailCalls != 1) {
            throw new AssertionError("low balance must configure recharge and notify");
        }
        System.out.println("BalanceGuardTest passed");
    }

    private static final class FakeClient extends InfraiClient {
        int configureCalls;
        int emailCalls;
        FakeClient() { super(null, "test", "http://localhost"); }
        @Override public String configureAutorecharge(double trigger, double amount) { configureCalls++; return "{\"ok\":true}"; }
        @Override public String sendEmail(String to, String subject, String body) { emailCalls++; return "{\"ok\":true}"; }
    }
}
