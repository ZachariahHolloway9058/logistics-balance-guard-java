package example.logistics;

public final class LogisticsExample {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) throw new IllegalArgumentException("Usage: LogisticsExample <notification-recipient>");
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
        InfraiClient client = new InfraiClient(key);
        LogisticsService service = new LogisticsService(new BalanceGuard(client, 25.0, args[0]));
        ProofOfDelivery pod = new ProofOfDelivery("SHP-1042", "pod-SHP-1042.pdf", "sha256:replace-with-upload-digest");
        System.out.println(service.accept(pod, 3.50));
    }
}
