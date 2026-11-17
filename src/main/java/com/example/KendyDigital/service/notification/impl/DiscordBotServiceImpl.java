package com.example.KendyDigital.service.notification.impl;

import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.monitoring.request.BanIpRequest;
import com.example.KendyDigital.dto.monitoring.request.FreezeWalletRequest;
import com.example.KendyDigital.dto.monitoring.response.SecurityAlertResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityApiKeyResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityEventResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityIpBanResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityIpProfileResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityRiskyAccountResponse;
import com.example.KendyDigital.dto.monitoring.response.SecuritySessionResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityTimelinePointResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityTopIpResponse;
import com.example.KendyDigital.dto.monitoring.response.SecurityUserRiskResponse;
import com.example.KendyDigital.dto.monitoring.response.ThreatOverviewResponse;
import com.example.KendyDigital.dto.user.response.AdminUserResponse;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.service.notification.DiscordBotService;
import com.example.KendyDigital.service.security.AdminSecurityManagerService;
import com.example.KendyDigital.service.security.AdminSecurityMonitorService;
import com.example.KendyDigital.service.security.monitor.AuditIntegrityService;
import com.example.KendyDigital.service.security.monitor.GeoIpDatabaseUpdater;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DiscordBotServiceImpl extends ListenerAdapter implements DiscordBotService {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotServiceImpl.class);

    @Value("${app.discord.bot-token:}")
    private String botToken;

    @Value("${app.discord.channel-id:}")
    private String channelId;

    private final OrderRepository orderRepository;
    private final AdminSecurityMonitorService securityMonitorService;
    private final AdminSecurityManagerService securityManagerService;
    private final GeoIpDatabaseUpdater geoIpDatabaseUpdater;

    private JDA jda;
    private volatile boolean isReady = false;

    public DiscordBotServiceImpl(
            OrderRepository orderRepository,
            AdminSecurityMonitorService securityMonitorService,
            AdminSecurityManagerService securityManagerService,
            GeoIpDatabaseUpdater geoIpDatabaseUpdater) {
        this.orderRepository = orderRepository;
        this.securityMonitorService = securityMonitorService;
        this.securityManagerService = securityManagerService;
        this.geoIpDatabaseUpdater = geoIpDatabaseUpdater;
    }

    @PostConstruct
    public void init() {
        if (botToken == null || botToken.isBlank()) {
            log.warn("[DiscordBot] Không tìm thấy Bot Token (app.discord.bot-token). Bỏ qua khởi động Discord Bot.");
            return;
        }

        try {
            log.info("[DiscordBot] Đang khởi động Discord Bot với JDA...");
            this.jda = JDABuilder.createDefault(botToken)
                    .addEventListeners(this)
                    .build();
        } catch (Exception e) {
            log.error("[DiscordBot] Không thể khởi động Discord Bot: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (jda != null) {
            log.info("[DiscordBot] Đang tắt kết nối Discord Bot...");
            jda.shutdown();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        this.isReady = true;
        log.info("[DiscordBot] Bot đã ONLINE thành công! Tên: {}#{}",
                event.getJDA().getSelfUser().getName(),
                event.getJDA().getSelfUser().getDiscriminator());

        List<CommandData> allCommands = buildAllCommands();

        // Đăng ký tức thì cho từng Guild (Server) để có hiệu lực ngay trong 0 giây
        for (Guild guild : event.getJDA().getGuilds()) {
            guild.updateCommands().addCommands(allCommands).queue(
                    success -> log.info("[DiscordBot] Đã đăng ký tức thì {} commands cho Server: {}",
                            success.size(), guild.getName()),
                    error -> log.warn("[DiscordBot] Lỗi đăng ký lệnh cho Server {}: {}",
                            guild.getName(), error.getMessage())
            );
        }

        // Đăng ký toàn cục
        event.getJDA().updateCommands().addCommands(allCommands).queue();

        // Gửi thông báo khởi động vào kênh cấu hình
        sendStartupNotification();
    }

    // =========================================================================
    // XÂY DỰNG BỘ LỆNH ĐẦY ĐỦ CHO DISCORD (CLI & AUTOCOMPLETE)
    // =========================================================================
    private List<CommandData> buildAllCommands() {
        List<CommandData> list = new ArrayList<>();

        // Reusable Options with Choice Suggestions
        OptionData severityOption = new OptionData(OptionType.STRING, "severity", "Lọc theo mức độ nghiêm trọng", false)
                .addChoice("🔥 CRITICAL (Khẩn cấp)", "CRITICAL")
                .addChoice("⚠️ HIGH (Mức độ cao)", "HIGH")
                .addChoice("⚡ MEDIUM (Trung bình)", "MEDIUM")
                .addChoice("ℹ️ LOW (Thấp)", "LOW");

        OptionData statusOption = new OptionData(OptionType.STRING, "status", "Lọc theo trạng thái xử lý", false)
                .addChoice("🔴 OPEN (Chưa xử lý)", "OPEN")
                .addChoice("🟡 INVESTIGATING (Đang điều tra)", "INVESTIGATING")
                .addChoice("🟢 RESOLVED (Đã xử lý)", "RESOLVED")
                .addChoice("⚪ DISMISSED (Bỏ qua)", "DISMISSED");

        OptionData categoryOption = new OptionData(OptionType.STRING, "category", "Phân loại nhật ký kiểm toán", false)
                .addChoice("🔐 AUTH (Xác thực / Đăng nhập)", "AUTH")
                .addChoice("💳 WEBHOOK (Thanh toán / SePay)", "WEBHOOK")
                .addChoice("⚙️ ADMIN (Hành động Quản trị)", "ADMIN")
                .addChoice("🛡️ SECURITY (Tấn công / WAF)", "SECURITY")
                .addChoice("🔑 CREDENTIAL (Mật khẩu / 2FA)", "CREDENTIAL")
                .addChoice("📦 ORDER (Xử lý đơn hàng)", "ORDER");

        OptionData hoursOption = new OptionData(OptionType.INTEGER, "hours", "Khoảng thời gian thống kê (giờ)", false)
                .addChoice("⏱️ 1 giờ qua", 1)
                .addChoice("⏱️ 6 giờ qua", 6)
                .addChoice("⏱️ 24 giờ qua (1 ngày)", 24)
                .addChoice("⏱️ 72 giờ qua (3 ngày)", 72)
                .addChoice("⏱️ 168 giờ qua (7 ngày)", 168);

        OptionData ipOptionAutoComplete = new OptionData(OptionType.STRING, "ip", "Địa chỉ IP (tự động gợi ý các IP bị cấm)", true)
                .setAutoComplete(true);

        OptionData commandOptionAutoComplete = new OptionData(OptionType.STRING, "command", "Tên lệnh cần xem hướng dẫn", false)
                .setAutoComplete(true);

        // 1. Root Utilities
        list.add(Commands.slash("ping", "🏓 Kiểm tra kết nối và độ trễ Gateway của Bot"));
        list.add(Commands.slash("status", "📈 Xem trạng thái phần cứng hệ thống (RAM, CPU, Đơn hàng)"));
        list.add(Commands.slash("help", "💡 Cẩm nang CLI & gợi ý lệnh an ninh (như docker/npm/git)")
                .addOptions(commandOptionAutoComplete));
        list.add(Commands.slash("order-check", "📦 Tra cứu trạng thái đơn hàng theo mã đơn")
                .addOption(OptionType.STRING, "code", "Mã đơn hàng (ví dụ: KD123456)", true));

        // 2. Subcommands Suite: /security and /sec (CLI style)
        List<SubcommandData> subcommands = buildSecuritySubcommands(severityOption, statusOption, categoryOption, hoursOption, ipOptionAutoComplete);
        list.add(Commands.slash("security", "🛡️ Bộ công cụ phân tích & giám sát an ninh KendyDigital").addSubcommands(subcommands));
        list.add(Commands.slash("sec", "⚡ Lệnh tắt CLI an ninh KendyDigital (như git/docker/npm)").addSubcommands(subcommands));

        // 3. Direct Root Commands for 1-Click Access
        list.add(Commands.slash("overview", "📊 Xem tổng quan an ninh, điểm số bảo mật và tấn công 24h qua"));
        list.add(Commands.slash("threats", "⚠️ Báo cáo chi tiết các mối đe dọa (WAF, Brute force, Webhook)"));
        list.add(Commands.slash("alerts", "🚨 Danh sách các cảnh báo bảo mật gần nhất")
                .addOptions(severityOption, statusOption));
        list.add(Commands.slash("events", "📜 Nhật ký chi tiết sự kiện an ninh (Security Events)")
                .addOption(OptionType.STRING, "type", "Loại sự kiện (WAF, RATE_LIMIT, LOGIN_FAIL...)")
                .addOptions(severityOption)
                .addOption(OptionType.STRING, "ip", "Lọc theo địa chỉ IP"));
        list.add(Commands.slash("top-ips", "🌐 Top địa chỉ IP tấn công và gửi yêu cầu nhiều nhất")
                .addOptions(hoursOption));
        list.add(Commands.slash("timeline", "📈 Biểu đồ sự kiện an ninh theo các mốc thời gian")
                .addOptions(hoursOption));
        list.add(Commands.slash("ip-check", "🔍 Tra cứu hồ sơ an ninh & mức độ rủi ro của 1 địa chỉ IP")
                .addOptions(ipOptionAutoComplete));
        list.add(Commands.slash("ip-bans", "🚫 Danh sách các địa chỉ IP đang bị hệ thống chặn (Banned IPs)"));
        list.add(Commands.slash("ip-ban", "🛑 Khóa một địa chỉ IP độc hại vào danh sách cấm")
                .addOption(OptionType.STRING, "ip", "Địa chỉ IP cần chặn", true)
                .addOption(OptionType.STRING, "reason", "Lý do chặn", false)
                .addOptions(hoursOption));
        list.add(Commands.slash("ip-unban", "🔓 Gỡ bỏ lệnh chặn cho một địa chỉ IP (gợi ý tự động)")
                .addOptions(ipOptionAutoComplete));
        list.add(Commands.slash("risky-accounts", "👤 Danh sách tài khoản có nguy cơ bị chiếm quyền (Account Takeover)"));
        list.add(Commands.slash("user-risk", "🎯 Tra cứu điểm số rủi ro của người dùng theo UUID")
                .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true));
        list.add(Commands.slash("user-unlock", "🔑 Mở khóa khẩn cấp tài khoản người dùng bị khóa")
                .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true));
        list.add(Commands.slash("wallet-freeze", "❄️ Đóng băng số dư ví tài khoản để ngăn tẩu tán tiền")
                .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true)
                .addOption(OptionType.STRING, "reason", "Lý do đóng băng ví", false));
        list.add(Commands.slash("wallet-unfreeze", "☀️ Mở đóng băng ví tài khoản")
                .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true));
        list.add(Commands.slash("sessions", "🖥️ Danh sách các phiên đăng nhập quản trị viên đang hoạt động"));
        list.add(Commands.slash("apikeys", "🔑 Danh sách các API Key hệ thống đang kích hoạt"));
        list.add(Commands.slash("admins", "👥 Danh sách tài khoản Quản trị viên & trạng thái 2FA"));
        list.add(Commands.slash("audit", "📜 Tra cứu nhật ký kiểm toán hệ thống (Audit Logs)")
                .addOptions(categoryOption));
        list.add(Commands.slash("integrity", "🔒 Kiểm tra tính toàn vẹn chuỗi băm (Hash Chain) của Audit Log"));
        list.add(Commands.slash("geoip-update", "🌍 Kích hoạt cập nhật cơ sở dữ liệu GeoIP MaxMind"));
        list.add(Commands.slash("test-alert", "🔔 Bắn thử một cảnh báo bảo mật mẫu vào kênh Discord"));

        return list;
    }

    private List<SubcommandData> buildSecuritySubcommands(
            OptionData severityOption, OptionData statusOption, OptionData categoryOption,
            OptionData hoursOption, OptionData ipOptionAutoComplete) {
        return List.of(
                new SubcommandData("overview", "📊 Xem tổng quan an ninh, điểm số bảo mật và tấn công 24h qua"),
                new SubcommandData("threats", "⚠️ Báo cáo chi tiết các mối đe dọa (WAF, Brute force, Webhook)"),
                new SubcommandData("alerts", "🚨 Danh sách các cảnh báo bảo mật gần nhất")
                        .addOptions(severityOption, statusOption),
                new SubcommandData("events", "📜 Nhật ký chi tiết sự kiện an ninh (Security Events)")
                        .addOption(OptionType.STRING, "type", "Loại sự kiện")
                        .addOptions(severityOption)
                        .addOption(OptionType.STRING, "ip", "Lọc theo địa chỉ IP"),
                new SubcommandData("top-ips", "🌐 Top địa chỉ IP tấn công và gửi yêu cầu nhiều nhất")
                        .addOptions(hoursOption),
                new SubcommandData("timeline", "📈 Biểu đồ sự kiện an ninh theo các mốc thời gian")
                        .addOptions(hoursOption),
                new SubcommandData("ip-check", "🔍 Tra cứu hồ sơ an ninh & mức độ rủi ro của 1 địa chỉ IP")
                        .addOptions(ipOptionAutoComplete),
                new SubcommandData("ip-bans", "🚫 Danh sách các địa chỉ IP đang bị hệ thống chặn (Banned IPs)"),
                new SubcommandData("ip-ban", "🛑 Khóa một địa chỉ IP độc hại vào danh sách cấm")
                        .addOption(OptionType.STRING, "ip", "Địa chỉ IP cần chặn", true)
                        .addOption(OptionType.STRING, "reason", "Lý do chặn", false)
                        .addOptions(hoursOption),
                new SubcommandData("ip-unban", "🔓 Gỡ bỏ lệnh chặn cho một địa chỉ IP (gợi ý tự động)")
                        .addOptions(ipOptionAutoComplete),
                new SubcommandData("risky-accounts", "👤 Danh sách tài khoản có nguy cơ bị chiếm quyền (ATO)"),
                new SubcommandData("user-risk", "🎯 Tra cứu điểm số rủi ro của người dùng theo UUID")
                        .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true),
                new SubcommandData("user-unlock", "🔑 Mở khóa khẩn cấp tài khoản người dùng bị khóa")
                        .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true),
                new SubcommandData("wallet-freeze", "❄️ Đóng băng số dư ví tài khoản để ngăn tẩu tán tiền")
                        .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true)
                        .addOption(OptionType.STRING, "reason", "Lý do đóng băng ví", false),
                new SubcommandData("wallet-unfreeze", "☀️ Mở đóng băng ví tài khoản")
                        .addOption(OptionType.STRING, "user_id", "UUID tài khoản người dùng", true),
                new SubcommandData("sessions", "🖥️ Danh sách các phiên đăng nhập quản trị viên đang hoạt động"),
                new SubcommandData("apikeys", "🔑 Danh sách các API Key hệ thống đang kích hoạt"),
                new SubcommandData("admins", "👥 Danh sách tài khoản Quản trị viên & trạng thái 2FA"),
                new SubcommandData("audit", "📜 Tra cứu nhật ký kiểm toán hệ thống (Audit Logs)")
                        .addOptions(categoryOption),
                new SubcommandData("integrity", "🔒 Kiểm tra tính toàn vẹn chuỗi băm (Hash Chain) của Audit Log"),
                new SubcommandData("geoip-update", "🌍 Kích hoạt cập nhật cơ sở dữ liệu GeoIP MaxMind"),
                new SubcommandData("help", "💡 Cẩm nang hướng dẫn lệnh CLI"),
                new SubcommandData("test-alert", "🔔 Bắn thử một cảnh báo bảo mật mẫu vào kênh Discord")
        );
    }

    // =========================================================================
    // DISCORD INTERACTION AUTO-COMPLETE (GỢI Ý TỰ ĐỘNG NHƯ CLI DOCKER/GIT/NPM)
    // =========================================================================
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String focusedOption = event.getFocusedOption().getName();
        String query = event.getFocusedOption().getValue().toLowerCase();

        if ("ip".equalsIgnoreCase(focusedOption)) {
            try {
                List<SecurityIpBanResponse> bans = securityMonitorService.ipBans(25);
                List<Command.Choice> choices = bans.stream()
                        .filter(b -> b.ipOrCidr().toLowerCase().contains(query) ||
                                (b.reason() != null && b.reason().toLowerCase().contains(query)))
                        .limit(25)
                        .map(b -> {
                            String reasonSummary = b.reason() != null ?
                                    (b.reason().length() > 25 ? b.reason().substring(0, 25) + "..." : b.reason()) : "Banned";
                            return new Command.Choice(b.ipOrCidr() + " (" + reasonSummary + ")", b.ipOrCidr());
                        })
                        .toList();
                event.replyChoices(choices).queue();
                return;
            } catch (Exception e) {
                log.debug("Lỗi autocomplete IP: {}", e.getMessage());
            }
        } else if ("command".equalsIgnoreCase(focusedOption)) {
            List<String> commandNames = List.of(
                    "overview", "threats", "alerts", "events", "top-ips", "timeline",
                    "ip-check", "ip-bans", "ip-ban", "ip-unban", "risky-accounts",
                    "user-risk", "user-unlock", "wallet-freeze", "wallet-unfreeze",
                    "sessions", "apikeys", "admins", "audit", "integrity",
                    "geoip-update", "test-alert", "status", "ping", "order-check"
            );
            List<Command.Choice> choices = commandNames.stream()
                    .filter(cmd -> cmd.toLowerCase().contains(query))
                    .limit(25)
                    .map(cmd -> new Command.Choice(cmd, cmd))
                    .toList();
            event.replyChoices(choices).queue();
            return;
        }

        event.replyChoices(List.of()).queue();
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN TƯƠNG TÁC LỆNH SLASH
    // =========================================================================
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        try {
            switch (commandName) {
                case "ping" -> handlePingCommand(event);
                case "status" -> handleStatusCommand(event);
                case "help" -> handleHelpCommand(event);
                case "order-check" -> handleOrderCheckCommand(event);
                case "security", "sec" -> handleSecurityCommand(event);
                case "overview" -> handleSecurityOverview(event);
                case "threats" -> handleSecurityThreats(event);
                case "alerts" -> handleSecurityAlerts(event);
                case "events" -> handleSecurityEvents(event);
                case "top-ips" -> handleSecurityTopIps(event);
                case "timeline" -> handleSecurityTimeline(event);
                case "ip-check" -> handleSecurityIpCheck(event);
                case "ip-bans" -> handleSecurityIpBans(event);
                case "ip-ban" -> handleSecurityIpBan(event);
                case "ip-unban" -> handleSecurityIpUnban(event);
                case "risky-accounts" -> handleSecurityRiskyAccounts(event);
                case "user-risk" -> handleSecurityUserRisk(event);
                case "user-unlock" -> handleSecurityUserUnlock(event);
                case "wallet-freeze" -> handleSecurityWalletFreeze(event);
                case "wallet-unfreeze" -> handleSecurityWalletUnfreeze(event);
                case "sessions" -> handleSecuritySessions(event);
                case "apikeys" -> handleSecurityApiKeys(event);
                case "admins" -> handleSecurityAdmins(event);
                case "audit" -> handleSecurityAudit(event);
                case "integrity" -> handleSecurityIntegrity(event);
                case "geoip-update" -> handleSecurityGeoIpUpdate(event);
                case "test-alert" -> handleSecurityTestAlert(event);
                default -> event.reply("⚠️ Lệnh không xác định! Gõ `/help` để xem danh sách gợi ý.").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            log.error("[DiscordBot] Lỗi khi xử lý lệnh {}: {}", commandName, e.getMessage(), e);
            event.reply("❌ Lỗi khi thực thi lệnh: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handlePingCommand(SlashCommandInteractionEvent event) {
        long gatewayPing = event.getJDA().getGatewayPing();
        event.reply("🏓 **Pong!** Bot KendyDigital đang hoạt động ổn định.\n⚡ Độ trễ Gateway: `" + gatewayPing + "ms`")
                .queue();
    }

    private void handleStatusCommand(SlashCommandInteractionEvent event) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        long uptimeMinutes = ManagementFactory.getRuntimeMXBean().getUptime() / (1000 * 60);

        long pendingPaymentOrders = orderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long processingOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📊 TRẠNG THÁI HỆ THỐNG KENDYDIGITAL");
        embed.setColor(new Color(0x3B82F6));
        embed.addField("⏰ Thời gian chạy", uptimeMinutes + " phút", true);
        embed.addField("☕ Java Version", System.getProperty("java.version"), true);
        embed.addField("💾 Bộ nhớ RAM", usedMemory + "MB / " + totalMemory + "MB", true);
        embed.addField("⏳ Đơn chờ thanh toán", String.valueOf(pendingPaymentOrders), true);
        embed.addField("⚙️ Đơn đang xử lý", String.valueOf(processingOrders), true);
        embed.addField("🟢 Trạng thái Bot", "Online 24/7 (JDA)", true);
        embed.setFooter("KendyDigital Monitoring System");
        embed.setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleOrderCheckCommand(SlashCommandInteractionEvent event) {
        String code = event.getOption("code") != null ? event.getOption("code").getAsString().trim() : "";
        if (code.isBlank()) {
            event.reply("Vui lòng nhập mã đơn hàng!").setEphemeral(true).queue();
            return;
        }

        Optional<OrderRecord> orderOpt = orderRepository.findByOrderCode(code);
        if (orderOpt.isEmpty()) {
            event.reply("❌ Không tìm thấy đơn hàng với mã: `" + code + "`").setEphemeral(true).queue();
            return;
        }

        OrderRecord order = orderOpt.get();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📦 THÔNG TIN ĐƠN HÀNG #" + order.getOrderCode());

        Color color = switch (order.getStatus()) {
            case COMPLETED, DELIVERED -> new Color(0x10B981);
            case CANCELLED, FAILED, REFUNDED -> new Color(0xEF4444);
            default -> new Color(0xF59E0B);
        };
        embed.setColor(color);

        embed.addField("Trạng thái", order.getStatus().name(), true);
        embed.addField("Dịch vụ", order.getService() != null ? order.getService().getName() : "N/A", true);
        embed.addField("Khách hàng ID", String.valueOf(order.getUser().getId()), true);
        embed.addField("Admin xử lý", order.getAssignedAdmin() != null ? order.getAssignedAdmin().getEmail() : "Chưa gán", true);
        embed.addField("Deadline", order.getProcessingDeadlineAt() != null ? order.getProcessingDeadlineAt().toString() : "Không có", true);
        embed.setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    // =========================================================================
    // XỬ LÝ NHÁNH LỆNH CLI: /security <subcommand> HOẶC /sec <subcommand>
    // =========================================================================
    private void handleSecurityCommand(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            handleHelpCommand(event);
            return;
        }

        switch (subcommand) {
            case "overview" -> handleSecurityOverview(event);
            case "threats" -> handleSecurityThreats(event);
            case "alerts" -> handleSecurityAlerts(event);
            case "events" -> handleSecurityEvents(event);
            case "top-ips" -> handleSecurityTopIps(event);
            case "timeline" -> handleSecurityTimeline(event);
            case "ip-check" -> handleSecurityIpCheck(event);
            case "ip-bans" -> handleSecurityIpBans(event);
            case "ip-ban" -> handleSecurityIpBan(event);
            case "ip-unban" -> handleSecurityIpUnban(event);
            case "risky-accounts" -> handleSecurityRiskyAccounts(event);
            case "user-risk" -> handleSecurityUserRisk(event);
            case "user-unlock" -> handleSecurityUserUnlock(event);
            case "wallet-freeze" -> handleSecurityWalletFreeze(event);
            case "wallet-unfreeze" -> handleSecurityWalletUnfreeze(event);
            case "sessions" -> handleSecuritySessions(event);
            case "apikeys" -> handleSecurityApiKeys(event);
            case "admins" -> handleSecurityAdmins(event);
            case "audit" -> handleSecurityAudit(event);
            case "integrity" -> handleSecurityIntegrity(event);
            case "geoip-update" -> handleSecurityGeoIpUpdate(event);
            case "help" -> handleHelpCommand(event);
            case "test-alert" -> handleSecurityTestAlert(event);
            default -> event.reply("⚠️ Lệnh con không hợp lệ: " + subcommand).setEphemeral(true).queue();
        }
    }

    // =========================================================================
    // CÁC HÀM XỬ LÝ TỪNG LỆNH BẢO MẬT CHI TIẾT
    // =========================================================================

    private void handleSecurityOverview(SlashCommandInteractionEvent event) {
        ThreatOverviewResponse threat = securityMonitorService.threatOverview();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🛡️ TỔNG QUAN BẢO MẬT HỆ THỐNG KENDYDIGITAL");
        int score = threat.healthScore();
        embed.setColor(score >= 80 ? new Color(0x10B981) : (score >= 50 ? new Color(0xF59E0B) : new Color(0xEF4444)));

        embed.addField("Điểm an toàn (Health)", score + " / 100", true);
        embed.addField("IP đang bị chặn", String.valueOf(threat.bannedIps()), true);
        embed.addField("Cảnh báo mở", String.valueOf(threat.openAlerts()), true);
        embed.addField("Cảnh báo Khẩn cấp", String.valueOf(threat.criticalAlerts()), true);
        embed.addField("Cảnh báo Nghiêm trọng", String.valueOf(threat.highAlerts()), true);
        embed.addField("Sự kiện an ninh 24h", String.valueOf(threat.securityEvents24h()), true);
        embed.addField("Lỗi đăng nhập 24h", String.valueOf(threat.loginFailures24h()), true);
        embed.addField("Chặn Rate Limit 24h", String.valueOf(threat.rateLimitEvents24h()), true);
        embed.addField("Chặn WAF 24h", String.valueOf(threat.wafEvents24h()), true);
        embed.addField("Từ chối Webhook 24h", String.valueOf(threat.webhookRejections24h()), true);
        embed.addField("Dạng tấn công chủ đạo", threat.topAttackType() != null ? threat.topAttackType() : "Không có", true);

        embed.setFooter("Hệ thống phát hiện xâm nhập & tường lửa KendyDigital");
        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityThreats(SlashCommandInteractionEvent event) {
        ThreatOverviewResponse threat = securityMonitorService.threatOverview();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("⚠️ BÁO CÁO CÁC MỐI ĐE DỌA TRONG 24 GIỜ");
        embed.setColor(new Color(0xDC2626));

        StringBuilder sb = new StringBuilder();
        sb.append("• **Tấn công WAF chặn được:** `").append(threat.wafEvents24h()).append("` lần\n");
        sb.append("• **Dò mật khẩu / Brute Force:** `").append(threat.loginFailures24h()).append("` lần\n");
        sb.append("• **Vượt ngưỡng Rate Limit:** `").append(threat.rateLimitEvents24h()).append("` lần\n");
        sb.append("• **Giả mạo Webhook SePay:** `").append(threat.webhookRejections24h()).append("` lần\n");
        sb.append("• **Mối đe dọa lớn nhất hiện tại:** `").append(threat.topAttackType() != null ? threat.topAttackType() : "An toàn").append("`\n");

        embed.setDescription(sb.toString());

        if (threat.latestAlerts() != null && !threat.latestAlerts().isEmpty()) {
            StringBuilder alertList = new StringBuilder();
            threat.latestAlerts().stream().limit(5).forEach(a ->
                    alertList.append("▸ [").append(a.severity()).append("] **")
                            .append(a.title()).append("** (x").append(a.eventCount()).append(")\n")
            );
            embed.addField("Cảnh báo mới nhất", alertList.toString(), false);
        }

        embed.setFooter("KendyDigital Threat Detection Engine");
        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityAlerts(SlashCommandInteractionEvent event) {
        String severity = getOptionString(event.getOption("severity"), "ALL");
        String status = getOptionString(event.getOption("status"), "ALL");

        String sevParam = "ALL".equalsIgnoreCase(severity) ? null : severity;
        String statParam = "ALL".equalsIgnoreCase(status) ? null : status;

        List<SecurityAlertResponse> alerts = securityMonitorService.alerts(statParam, sevParam, 8);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🚨 DANH SÁCH CẢNH BÁO BẢO MẬT (Lọc: " + severity + " / " + status + ")");
        embed.setColor(new Color(0xF97316));

        if (alerts.isEmpty()) {
            embed.setDescription("✅ Không có cảnh báo bảo mật nào thỏa mãn điều kiện lọc.");
        } else {
            for (SecurityAlertResponse a : alerts) {
                String val = String.format("Mức độ: `%s` | Trạng thái: `%s` | Số lần: `%d`\nĐối tượng: `%s`",
                        a.severity(), a.status(), a.eventCount(), a.subjectValue() != null ? a.subjectValue() : "N/A");
                embed.addField(a.ruleCode() + " - " + a.title(), val, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityIpBans(SlashCommandInteractionEvent event) {
        List<SecurityIpBanResponse> bans = securityMonitorService.ipBans(10);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🚫 DANH SÁCH CÁC ĐỊA CHỈ IP ĐANG BỊ CẤM");
        embed.setColor(new Color(0x991B1B));

        if (bans.isEmpty()) {
            embed.setDescription("✅ Hiện tại không có IP nào nằm trong danh sách cấm.");
        } else {
            for (SecurityIpBanResponse ban : bans) {
                String info = String.format("Lý do: **%s** | Nguồn: `%s` | Vi phạm: `%d` lần\nHết hạn: `%s`",
                        ban.reason(), ban.source(), ban.hitCount(),
                        ban.expiresAt() != null ? ban.expiresAt().toString() : "Vĩnh viễn");
                embed.addField("IP: " + ban.ipOrCidr(), info, false);
            }
        }

        embed.setFooter("KendyDigital Firewall & Auto-ban");
        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityIpCheck(SlashCommandInteractionEvent event) {
        String ip = event.getOption("ip") != null ? event.getOption("ip").getAsString().trim() : "";
        if (ip.isBlank()) {
            event.reply("❌ Vui lòng cung cấp địa chỉ IP cần kiểm tra!").setEphemeral(true).queue();
            return;
        }

        SecurityIpProfileResponse profile = securityMonitorService.ipProfile(ip, 5);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔍 HỒ SƠ AN NINH IP: " + ip);
        embed.setColor(profile.banned() ? new Color(0xEF4444) : (profile.riskScore() > 50 ? new Color(0xF59E0B) : new Color(0x10B981)));

        embed.addField("Điểm rủi ro (Risk)", String.format("%.1f / 100", profile.riskScore()), true);
        embed.addField("Trạng thái Cấm", profile.banned() ? "🔴 Đang bị cấm" : "🟢 Không bị cấm", true);
        embed.addField("Danh sách trắng (Whitelist)", profile.allowlisted() ? "Có" : "Không", true);
        embed.addField("Tổng sự kiện ghi nhận", String.valueOf(profile.eventCount()), true);
        embed.addField("Lần đầu xuất hiện", profile.firstSeen() != null ? profile.firstSeen().toString() : "N/A", true);
        embed.addField("Lần cuối truy cập", profile.lastSeen() != null ? profile.lastSeen().toString() : "N/A", true);

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityIpBan(SlashCommandInteractionEvent event) {
        String ip = event.getOption("ip") != null ? event.getOption("ip").getAsString().trim() : "";
        String reason = getOptionString(event.getOption("reason"), "Chặn thủ công qua Discord Bot");
        int hours = event.getOption("hours") != null ? (int) event.getOption("hours").getAsLong() : 24;

        if (ip.isBlank()) {
            event.reply("❌ Vui lòng cung cấp địa chỉ IP cần chặn!").setEphemeral(true).queue();
            return;
        }

        SecurityIpBanResponse ban = securityMonitorService.banIp(null, new BanIpRequest(ip, reason, hours * 60));

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🛑 ĐÃ CHẶN ĐỊA CHỈ IP VÀO FIREWALL");
        embed.setColor(new Color(0xEF4444));
        embed.addField("Địa chỉ IP", "`" + ban.ipOrCidr() + "`", true);
        embed.addField("Thời hạn", hours + " giờ", true);
        embed.addField("Lý do chặn", ban.reason(), false);
        embed.setFooter("KendyDigital Firewall Active Rule");
        embed.setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityIpUnban(SlashCommandInteractionEvent event) {
        String ip = event.getOption("ip") != null ? event.getOption("ip").getAsString().trim() : "";
        if (ip.isBlank()) {
            event.reply("❌ Vui lòng cung cấp địa chỉ IP cần gỡ chặn!").setEphemeral(true).queue();
            return;
        }

        List<SecurityIpBanResponse> bans = securityMonitorService.ipBans(100);
        Optional<SecurityIpBanResponse> match = bans.stream()
                .filter(b -> b.ipOrCidr().equalsIgnoreCase(ip) || b.id().toString().equalsIgnoreCase(ip))
                .findFirst();

        if (match.isEmpty()) {
            event.reply("⚠️ Không tìm thấy lệnh cấm nào đang hoạt động cho IP: `" + ip + "`").setEphemeral(true).queue();
            return;
        }

        securityMonitorService.unbanIp(null, match.get().id());

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔓 ĐÃ GỠ CHẶN ĐỊA CHỈ IP THÀNH CÔNG");
        embed.setColor(new Color(0x10B981));
        embed.addField("Địa chỉ IP", "`" + match.get().ipOrCidr() + "`", true);
        embed.addField("Lý do trước đó", match.get().reason(), false);
        embed.setFooter("KendyDigital Firewall Rule Revoked");
        embed.setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityRiskyAccounts(SlashCommandInteractionEvent event) {
        List<SecurityRiskyAccountResponse> accounts = securityMonitorService.riskyAccounts(8);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("👤 TÀI KHOẢN CÓ NGUY CƠ CAO (ACCOUNT TAKEOVER)");
        embed.setColor(new Color(0xD97706));

        if (accounts.isEmpty()) {
            embed.setDescription("✅ Không có tài khoản nào có dấu hiệu bất thường.");
        } else {
            for (SecurityRiskyAccountResponse acc : accounts) {
                String detail = String.format("Email: `%s` | Vai trò: `%s`\nSai MK: `%d` lần | Khóa: `%s` | Đóng băng ví: `%s`\nUUID: `%s`",
                        acc.email(), acc.role(), acc.failedLoginAttempts(),
                        acc.locked() ? "Đã khóa" : "Bình thường",
                        acc.walletFrozen() ? "Đã đóng băng" : "Bình thường",
                        acc.userId());
                embed.addField(acc.name(), detail, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityUserRisk(SlashCommandInteractionEvent event) {
        String userIdStr = event.getOption("user_id") != null ? event.getOption("user_id").getAsString().trim() : "";
        if (userIdStr.isBlank()) {
            event.reply("❌ Vui lòng nhập User UUID!").setEphemeral(true).queue();
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            SecurityUserRiskResponse risk = securityMonitorService.userRisk(userId, 5);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🎯 ĐÁNH GIÁ RỦI RO NGƯỜI DÙNG: " + risk.name());
            embed.setColor(risk.riskScore() >= 60 ? new Color(0xEF4444) : (risk.riskScore() >= 30 ? new Color(0xF59E0B) : new Color(0x10B981)));

            embed.addField("Email", risk.email(), true);
            embed.addField("Vai trò", risk.role(), true);
            embed.addField("Điểm rủi ro", String.format("%.1f / 100", risk.riskScore()), true);
            embed.addField("Số lần sai mật khẩu", String.valueOf(risk.failedLoginAttempts()), true);
            embed.addField("Trạng thái Khóa", risk.locked() ? "🔴 Đã khóa" : "🟢 Hoạt động", true);
            embed.addField("Đóng băng Ví", risk.walletFrozen() ? "❄️ Đã đóng băng" : "Bình thường", true);
            embed.addField("Bảo mật 2FA", risk.twoFactorEnabled() ? "🛡️ Đã bật" : "⚠️ Chưa bật", true);
            embed.addField("IP đăng nhập gần nhất", risk.lastLoginIp() != null ? risk.lastLoginIp() + " (" + risk.lastLoginCountry() + ")" : "N/A", true);
            embed.addField("Sự kiện an ninh 24h", String.valueOf(risk.securityEvents24h()), true);

            embed.setTimestamp(Instant.now());
            event.replyEmbeds(embed.build()).queue();
        } catch (IllegalArgumentException e) {
            event.reply("❌ Định dạng UUID không hợp lệ! (Ví dụ: `123e4567-e89b-12d3-a456-426614174000`)").setEphemeral(true).queue();
        }
    }

    private void handleSecurityUserUnlock(SlashCommandInteractionEvent event) {
        String userIdStr = event.getOption("user_id") != null ? event.getOption("user_id").getAsString().trim() : "";
        if (userIdStr.isBlank()) {
            event.reply("❌ Vui lòng nhập User UUID cần mở khóa!").setEphemeral(true).queue();
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            SecurityRiskyAccountResponse unlocked = securityMonitorService.unlockAccount(null, userId);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🔑 ĐÃ MỞ KHÓA TÀI KHOẢN THÀNH CÔNG");
            embed.setColor(new Color(0x10B981));
            embed.addField("Người dùng", unlocked.name() + " (`" + unlocked.email() + "`)", false);
            embed.addField("Trạng thái", "Đã mở khóa & đặt lại số lần sai mật khẩu về 0", false);
            embed.setTimestamp(Instant.now());
            event.replyEmbeds(embed.build()).queue();
        } catch (Exception e) {
            event.reply("❌ Lỗi khi mở khóa tài khoản: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handleSecurityWalletFreeze(SlashCommandInteractionEvent event) {
        String userIdStr = event.getOption("user_id") != null ? event.getOption("user_id").getAsString().trim() : "";
        String reason = getOptionString(event.getOption("reason"), "Đóng băng khẩn cấp qua Discord Bot");

        if (userIdStr.isBlank()) {
            event.reply("❌ Vui lòng nhập User UUID!").setEphemeral(true).queue();
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            SecurityUserRiskResponse resp = securityMonitorService.freezeWallet(null, userId, new FreezeWalletRequest(reason));

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("❄️ ĐÃ ĐÓNG BĂNG VÍ TIỀN NGƯỜI DÙNG");
            embed.setColor(new Color(0x3B82F6));
            embed.addField("Người dùng", resp.name() + " (`" + resp.email() + "`)", false);
            embed.addField("Lý do", reason, false);
            embed.addField("Trạng thái ví", "ĐÃ ĐÓNG BĂNG (Không thể rút hoặc thanh toán)", false);
            embed.setTimestamp(Instant.now());
            event.replyEmbeds(embed.build()).queue();
        } catch (Exception e) {
            event.reply("❌ Lỗi khi đóng băng ví: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handleSecurityWalletUnfreeze(SlashCommandInteractionEvent event) {
        String userIdStr = event.getOption("user_id") != null ? event.getOption("user_id").getAsString().trim() : "";
        if (userIdStr.isBlank()) {
            event.reply("❌ Vui lòng nhập User UUID!").setEphemeral(true).queue();
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            SecurityUserRiskResponse resp = securityMonitorService.unfreezeWallet(null, userId, new FreezeWalletRequest("Mở khóa qua Discord Bot"));

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("☀️ ĐÃ MỞ ĐÓNG BĂNG VÍ TIỀN");
            embed.setColor(new Color(0x10B981));
            embed.addField("Người dùng", resp.name() + " (`" + resp.email() + "`)", false);
            embed.addField("Trạng thái ví", "BÌNH THƯỜNG (Có thể giao dịch bình thường)", false);
            embed.setTimestamp(Instant.now());
            event.replyEmbeds(embed.build()).queue();
        } catch (Exception e) {
            event.reply("❌ Lỗi khi mở đóng băng ví: " + e.getMessage()).setEphemeral(true).queue();
        }
    }

    private void handleSecuritySessions(SlashCommandInteractionEvent event) {
        List<SecuritySessionResponse> sessions = securityMonitorService.activeSessions(0, 8);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🖥️ CÁC PHIÊN ĐĂNG NHẬP ADMIN ĐANG HOẠT ĐỘNG");
        embed.setColor(new Color(0x6366F1));

        if (sessions.isEmpty()) {
            embed.setDescription("Không có phiên quản trị viên nào đang hoạt động.");
        } else {
            for (SecuritySessionResponse s : sessions) {
                String detail = String.format("Admin: **%s** (`%s`)\nIP: `%s` (%s)\nThiết bị: `%s`\nHết hạn: `%s`",
                        s.userName(), s.userEmail(), s.lastIp(),
                        s.country() != null ? s.country() : "VN",
                        s.userAgent() != null ? (s.userAgent().length() > 40 ? s.userAgent().substring(0, 40) + "..." : s.userAgent()) : "N/A",
                        s.expiresAt() != null ? s.expiresAt().toString() : "N/A");
                embed.addField("Phiên: " + s.id().toString().substring(0, 8) + "...", detail, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityApiKeys(SlashCommandInteractionEvent event) {
        List<SecurityApiKeyResponse> keys = securityMonitorService.activeApiKeys(0, 10);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔑 DANH SÁCH API KEY ĐANG KÍCH HOẠT");
        embed.setColor(new Color(0x8B5CF6));

        if (keys.isEmpty()) {
            embed.setDescription("Không có API Key nào đang hoạt động.");
        } else {
            for (SecurityApiKeyResponse k : keys) {
                String detail = String.format("Chủ sở hữu: **%s** (`%s`)\nPrefix: `%s...` | Scopes: `%s`\nDùng lần cuối: `%s`",
                        k.userName(), k.userEmail(), k.keyPrefix(),
                        k.scopes() != null ? k.scopes() : "Full Access",
                        k.lastUsedAt() != null ? k.lastUsedAt().toString() : "Chưa sử dụng");
                embed.addField("Key: " + k.name(), detail, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityAdmins(SlashCommandInteractionEvent event) {
        List<AdminUserResponse> admins = securityManagerService.listAdmins(15);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("👥 DANH SÁCH QUẢN TRỊ VIÊN HỆ THỐNG");
        embed.setColor(new Color(0x6366F1));

        if (admins.isEmpty()) {
            embed.setDescription("Không có tài khoản quản trị viên nào.");
        } else {
            for (AdminUserResponse adm : admins) {
                String detail = String.format("Email: `%s` | Vai trò: `%s`\nTrạng thái: `%s` | 2FA: `%s`\nID: `%s`",
                        adm.email(), adm.role(), adm.status(),
                        adm.twoFactorEnabled() ? "🟢 Đã bật" : "🔴 Chưa bật",
                        adm.id());
                embed.addField(adm.name(), detail, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityAudit(SlashCommandInteractionEvent event) {
        String category = getOptionString(event.getOption("category"), "ALL");
        String catParam = "ALL".equalsIgnoreCase(category) ? null : category;

        List<AuditLogResponse> logs = securityMonitorService.events(catParam, 8);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📜 NHẬT KÝ KIỂM TOÁN (AUDIT LOGS - " + category + ")");
        embed.setColor(new Color(0x0284C7));

        if (logs.isEmpty()) {
            embed.setDescription("Không có bản ghi nhật ký kiểm toán nào.");
        } else {
            for (AuditLogResponse l : logs) {
                String detail = String.format("Thao tác: **%s** | Vai trò: `%s` (ID: %d)\nIP: `%s` | Mục tiêu: `%s` (ID: %s)\nThời gian: `%s`",
                        l.action(), l.actorRole(), l.actorUserId() != null ? l.actorUserId() : 0,
                        l.ipAddress(), l.targetType(), l.targetId() != null ? String.valueOf(l.targetId()) : "N/A",
                        l.createdAt().toString());
                embed.addField("Audit #" + l.id().toString().substring(0, 8), detail, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityIntegrity(SlashCommandInteractionEvent event) {
        AuditIntegrityService.IntegrityReport report = securityMonitorService.auditIntegrity();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔒 KIỂM TRA TÍNH TOÀN VẸN CHUỖI BĂM (AUDIT INTEGRITY)");
        if (report.intact()) {
            embed.setColor(new Color(0x10B981));
            embed.setDescription("✅ **CHUỖI BĂM BẢO MẬT NGUYÊN VẸN!**\nKhông có dấu hiệu sửa đổi hoặc xóa trộm dữ liệu kiểm toán trong cơ sở dữ liệu.");
        } else {
            embed.setColor(new Color(0xEF4444));
            embed.setDescription("🚨 **CẢNH BÁO: CHUỖI BĂM BỊ PHÁ VỠ!**\nPhát hiện bản ghi bị chỉnh sửa hoặc xóa trực tiếp trong database!\nBản ghi lỗi đầu tiên: ID #" + report.firstBrokenId());
        }

        embed.addField("Số bản ghi đã kiểm tra", String.valueOf(report.checkedRows()), true);
        embed.addField("Trạng thái chuỗi", report.intact() ? "INTACT (Nguyên vẹn)" : "COMPROMISED (Bị sửa đổi)", true);
        embed.setFooter("HMAC Cryptographic Hash-Chain Verification");
        embed.setTimestamp(Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityTopIps(SlashCommandInteractionEvent event) {
        int hours = event.getOption("hours") != null ? (int) event.getOption("hours").getAsLong() : 24;
        List<SecurityTopIpResponse> topIps = securityMonitorService.topIps(hours, 8);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🌐 TOP ĐỊA CHỈ IP TẤN CÔNG / GỬI YÊU CẦU (" + hours + "H QUA)");
        embed.setColor(new Color(0xDC2626));

        if (topIps.isEmpty()) {
            embed.setDescription("✅ Không ghi nhận địa chỉ IP tấn công nào trong " + hours + " giờ qua.");
        } else {
            int rank = 1;
            for (SecurityTopIpResponse ip : topIps) {
                String detail = String.format("Sự kiện: `%d` lần | Mức độ cao nhất: `%s` | Điểm rủi ro: `%.1f`\nQuốc gia: `%s` | ASN: `%s` | Trạng thái: %s",
                        ip.eventCount(), ip.maxSeverity(), ip.riskScore(),
                        ip.country() != null ? ip.country() : "N/A",
                        ip.asn() != null ? ip.asn() : "N/A",
                        ip.banned() ? "🔴 Đang bị cấm" : "🟢 Bình thường");
                embed.addField("#" + (rank++) + " IP: " + ip.ip(), detail, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityTimeline(SlashCommandInteractionEvent event) {
        int hours = event.getOption("hours") != null ? (int) event.getOption("hours").getAsLong() : 24;
        List<SecurityTimelinePointResponse> points = securityMonitorService.timeline(hours, 60);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📊 BIỂU ĐỒ SỰ KIỆN AN NINH THEO THỜI GIAN (" + hours + "H QUA)");
        embed.setColor(new Color(0x0284C7));

        if (points.isEmpty()) {
            embed.setDescription("Không có dữ liệu sự kiện an ninh trong " + hours + " giờ qua.");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("```\n");
            sb.append(String.format("%-16s | %-6s | %-6s | %-6s\n", "Mốc giờ", "Tổng", "WAF", "Sai MK"));
            sb.append("-----------------+--------+--------+-------\n");
            int count = 0;
            for (SecurityTimelinePointResponse p : points) {
                if (p.total() > 0 || count++ % 3 == 0) {
                    String timeStr = p.bucket().toString().length() >= 16 ? p.bucket().toString().substring(11, 16) : p.bucket().toString();
                    sb.append(String.format("%-16s | %-6d | %-6d | %-6d\n", timeStr, p.total(), p.wafHits(), p.loginFailures()));
                }
            }
            sb.append("```");
            embed.setDescription(sb.toString());
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityEvents(SlashCommandInteractionEvent event) {
        String type = getOptionString(event.getOption("type"), null);
        String severity = getOptionString(event.getOption("severity"), null);
        String ip = getOptionString(event.getOption("ip"), null);

        List<SecurityEventResponse> events = securityMonitorService.securityEvents(type, severity, ip, null, null, null, 8);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📜 NHẬT KÝ SỰ KIỆN BẢO MẬT (SECURITY EVENTS)");
        embed.setColor(new Color(0xF59E0B));

        if (events.isEmpty()) {
            embed.setDescription("Không có sự kiện bảo mật nào khớp với điều kiện tìm kiếm.");
        } else {
            for (SecurityEventResponse ev : events) {
                String detail = String.format("Loại: `%s` | Mức độ: `%s` | IP: `%s` (%s)\nĐường dẫn: `%s %s` | Số lần: `%d`\nThời gian: `%s`",
                        ev.type(), ev.severity(), ev.ip(), ev.country() != null ? ev.country() : "N/A",
                        ev.method() != null ? ev.method() : "", ev.path() != null ? ev.path() : "",
                        ev.eventCount(), ev.occurredAt().toString());
                embed.addField("Sự kiện #" + (ev.id() != null ? ev.id().toString().substring(0, 8) : "N/A"), detail, false);
            }
        }

        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityGeoIpUpdate(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        try {
            GeoIpDatabaseUpdater.UpdateResult result = geoIpDatabaseUpdater.updateAll();
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🌍 KẾT QUẢ CẬP NHẬT CƠ SỞ DỮ LIỆU GEOIP");
            embed.setColor(result.updated() ? new Color(0x10B981) : new Color(0xF59E0B));
            embed.addField("Trạng thái", result.updated() ? "✅ Đã cập nhật thành công" : "ℹ️ Đã kiểm tra (Không cần tải lại)", true);
            embed.addField("Chi tiết", result.message() != null ? result.message() : "Đã hoàn tất", false);
            embed.setTimestamp(Instant.now());
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("❌ Lỗi khi cập nhật cơ sở dữ liệu GeoIP: " + e.getMessage()).queue();
        }
    }

    private void handleHelpCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("💡 KENDYDIGITAL SECURITY CLI - BẢNG TRA CỨU & GỢI Ý LỆNH");
        embed.setColor(new Color(0x3B82F6));

        String cliGuide = """
```ansi
[1;36mKendyDigital Security CLI Suite v2.5 (Terminal Edition)[0m
[1;33mCú pháp gợi ý tương tự git, docker, npm:[0m
  /security <command> [options]   hoặc lệnh tắt:   /sec <command>
  /<command> [options]             (gõ trực tiếp như /threats, /alerts)

[1;32m[📊 GIÁM SÁT & TÌNH BÁO AN NINH][0m
  [1;37moverview[0m         Điểm số bảo mật, tổng quan tấn công 24h
  [1;37mthreats[0m          Báo cáo WAF, dò mật khẩu, rate limit, webhook
  [1;37malerts[0m           Danh sách cảnh báo [severity] [status]
  [1;37mevents[0m           Lịch sử sự kiện an ninh [type] [severity]
  [1;37mtop-ips[0m          Top IP tấn công nhiều nhất [hours: 1, 6, 24, 72, 168]
  [1;37mtimeline[0m         Biểu đồ phân bố sự kiện theo từng khung giờ

[1;32m[🛡️ TƯỜNG LỬA & QUẢN LÝ IP][0m
  [1;37mip-check <ip>[0m    Soi hồ sơ an ninh, điểm rủi ro & lịch sử của IP
  [1;37mip-bans[0m          Xem danh sách IP đang bị phong tỏa (Blacklist)
  [1;37mip-ban <ip>[0m      Chặn địa chỉ IP [ip] [reason] [hours]
  [1;37mip-unban <ip>[0m    Gỡ chặn IP ([1;33mcó gợi ý tự động danh sách IP bị cấm[0m)

[1;32m[👤 KIỂM SOÁT TÀI KHOẢN & VÍ][0m
  [1;37mrisky-accounts[0m   Tài khoản có dấu hiệu bị hack mật khẩu (ATO)
  [1;37muser-risk <id>[0m   Chấm điểm rủi ro người dùng theo User UUID
  [1;37muser-unlock <id>[0m Mở khóa khẩn cấp tài khoản người dùng
  [1;37mwallet-freeze[0m    Đóng băng số dư ví người dùng ngăn rút tiền
  [1;37mwallet-unfreeze[0m  Mở đóng băng ví người dùng

[1;32m[🔑 QUẢN TRỊ & HỆ THỐNG][0m
  [1;37msessions[0m         Xem phiên đăng nhập của Admin đang hoạt động
  [1;37mapikeys[0m          Danh sách API Key đang kích hoạt trên hệ thống
  [1;37madmins[0m           Danh sách tài khoản Quản trị viên & trạng thái 2FA
  [1;37maudit[0m            Nhật ký kiểm toán [category: AUTH, WEBHOOK, ADMIN...]
  [1;37mintegrity[0m        Kiểm tra toàn vẹn chuỗi băm Hash Chain SHA-256
  [1;37mgeoip-update[0m     Cập nhật cơ sở dữ liệu GeoIP MaxMind
  [1;37mtest-alert[0m       Bắn thử cảnh báo mẫu kiểm tra kết nối bot
```
""";

        embed.setDescription(cliGuide);
        embed.addField("Tip thông minh", "Khi bạn gõ lệnh (ví dụ `/ip-unban` hoặc `/alerts`), Discord sẽ **tự động gợi ý** danh sách IP đang cấm hoặc mức độ nguy hiểm ngay dưới con trỏ phím `Tab`!", false);
        embed.setFooter("KendyDigital Enterprise Security Suite");
        embed.setTimestamp(Instant.now());
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleSecurityTestAlert(SlashCommandInteractionEvent event) {
        sendAlert("🔔 CẢNH BÁO BẢO MẬT MẪU (TEST ALERT)",
                "Admin **@" + event.getUser().getName() + "** vừa kích hoạt lệnh thử nghiệm `/security test-alert` từ Discord!\nHệ thống kênh thông báo bảo mật đang hoạt động hoàn hảo.",
                0x10B981);
        event.reply("✅ Đã gửi thẻ cảnh báo mẫu vào kênh Discord!").setEphemeral(true).queue();
    }

    private String getOptionString(OptionMapping option, String defaultValue) {
        if (option == null || option.getAsString().isBlank()) return defaultValue;
        return option.getAsString().trim();
    }

    private void sendStartupNotification() {
        if (channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("🚀 KENDYDIGITAL SECURITY SUITE V2.5 ONLINE");
                embed.setDescription("Hệ thống backend đã đồng bộ toàn bộ bộ lệnh an ninh với Discord.\n\n" +
                        "💡 **Cách sử dụng:**\n" +
                        "• Gõ `/help` để xem toàn bộ danh mục lệnh CLI\n" +
                        "• Gõ `/sec ` hoặc `/security ` để chọn nhanh các lệnh con\n" +
                        "• Gõ trực tiếp: `/threats`, `/alerts`, `/ip-bans`, `/ip-check`, `/top-ips`, `/audit`, `/integrity`...\n" +
                        "• Tính năng tự động gợi ý (Autocomplete) đã được kích hoạt cho IP và các tham số lựa chọn!");
                embed.setColor(new Color(0x22C55E));
                embed.setTimestamp(Instant.now());
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            log.warn("[DiscordBot] Không thể gửi thông báo khởi động: {}", e.getMessage());
        }
    }

    @Override
    public void sendAlert(String title, String description, int colorHex) {
        if (!isReady || channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(title);
                embed.setDescription(description);
                embed.setColor(new Color(colorHex));
                embed.setTimestamp(Instant.now());
                embed.setFooter("KendyDigital Alert System");
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            log.error("[DiscordBot] Lỗi khi gửi thông báo: {}", e.getMessage());
        }
    }

    @Override
    public void sendOrderDeadlineAlert(String orderCode, String serviceName, Long userId, String deadline, boolean overdue) {
        if (!isReady || channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                int color = overdue ? 0xEF4444 : 0xF59E0B;
                String title = overdue ? "🚨 [QUÁ HẠN] ĐƠN HÀNG #" + orderCode : "⏰ [SẮP HẾT HẠN] ĐƠN HÀNG #" + orderCode;

                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(title);
                embed.setColor(new Color(color));
                embed.addField("Dịch vụ", serviceName != null ? serviceName : "N/A", true);
                embed.addField("Khách hàng ID", userId != null ? String.valueOf(userId) : "N/A", true);
                embed.addField("Hạn chót xử lý", deadline != null ? deadline : "N/A", false);
                embed.setFooter("KendyDigital Order Monitor");
                embed.setTimestamp(Instant.now());

                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            log.error("[DiscordBot] Lỗi gửi cảnh báo deadline: {}", e.getMessage());
        }
    }

    @Override
    public void sendSecurityAlert(String severity, String ruleCode, String title, String detail) {
        if (!isReady || channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                int color = "CRITICAL".equalsIgnoreCase(severity) ? 0xEF4444 : 0xF97316;
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("🚨 [" + severity + "] " + ruleCode + " — " + title);
                embed.setColor(new Color(color));
                if (detail != null && !detail.isBlank()) {
                    embed.setDescription("```\n" + (detail.length() > 500 ? detail.substring(0, 500) + "..." : detail) + "\n```");
                }
                embed.setFooter("KendyDigital Threat Detection Engine");
                embed.setTimestamp(Instant.now());
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            log.error("[DiscordBot] Lỗi gửi cảnh báo an ninh: {}", e.getMessage());
        }
    }

    @Override
    public void sendDepositAlert(String depositCode, java.math.BigDecimal amount, String customerName, String accountNumber, String transactionRef) {
        if (!isReady || channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("💰 NẠP TIỀN THÀNH CÔNG (SEPAY)");
                embed.setColor(new Color(0x10B981));
                embed.addField("Số tiền", String.format("%,.0f VNĐ", amount), true);
                embed.addField("Mã nạp", depositCode != null ? depositCode : "N/A", true);
                embed.addField("Khách hàng", customerName != null ? customerName : "Khách hàng", true);
                if (accountNumber != null && !accountNumber.isBlank()) {
                    embed.addField("STK Ngân hàng", accountNumber, true);
                }
                if (transactionRef != null && !transactionRef.isBlank()) {
                    embed.addField("Mã tham chiếu SePay", transactionRef, true);
                }
                embed.setFooter("KendyDigital Payment Gateway");
                embed.setTimestamp(Instant.now());
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            log.error("[DiscordBot] Lỗi gửi thông báo nạp tiền: {}", e.getMessage());
        }
    }

    @Override
    public void sendNewOrderAlert(String orderCode, String serviceName, Long userId, String price, boolean isManual) {
        if (!isReady || channelId == null || channelId.isBlank()) return;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null && channel.canTalk()) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle(isManual ? "📝 ĐƠN HÀNG THỦ CÔNG MỚI #" + orderCode : "📦 ĐƠN HÀNG MỚI #" + orderCode);
                embed.setColor(isManual ? new Color(0x8B5CF6) : new Color(0x3B82F6));
                embed.addField("Dịch vụ", serviceName != null ? serviceName : "N/A", true);
                embed.addField("Giá trị", price != null ? price : "N/A", true);
                embed.addField("Khách hàng ID", userId != null ? String.valueOf(userId) : "N/A", true);
                if (isManual) {
                    embed.addField("Yêu cầu xử lý", "Đơn hàng cần Admin gán tài khoản/xử lý theo brief của khách", false);
                }
                embed.setFooter("KendyDigital Order System");
                embed.setTimestamp(Instant.now());
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            log.error("[DiscordBot] Lỗi gửi thông báo đơn hàng mới: {}", e.getMessage());
        }
    }

    @Override
    public boolean isReady() {
        return isReady;
    }
}
